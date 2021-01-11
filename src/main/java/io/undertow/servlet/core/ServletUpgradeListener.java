package io.undertow.servlet.core;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ThreadSetupAction;
import io.undertow.servlet.spec.WebConnectionImpl;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;

import javax.servlet.http.HttpUpgradeHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Lister that handles a servlet exchange upgrade event.
 *
 * @author Stuart Douglas
 */
public class ServletUpgradeListener<T extends HttpUpgradeHandler> implements HttpUpgradeListener {
    private final InstanceHandle<T> instance;
    private final ThreadSetupAction threadSetupAction;
    private final HttpServerExchange exchange;

    public ServletUpgradeListener(final InstanceHandle<T> instance, ThreadSetupAction threadSetupAction, HttpServerExchange exchange) {
        this.instance = instance;
        this.threadSetupAction = threadSetupAction;
        this.exchange = exchange;
    }

    @Override
    public void handleUpgrade(final StreamConnection channel, final HttpServerExchange exchange) {
        channel.getCloseSetter().set(new ChannelListener<StreamConnection>() {
            @Override
            public void handleEvent(StreamConnection channel) {
                final ThreadSetupAction.Handle handle = threadSetupAction.setup(ServletUpgradeListener.this.exchange);
                try {
                    instance.getInstance().destroy();
                } finally {
                    try {
                        handle.tearDown();
                    } finally {
                        instance.release();
                    }
                }
            }
        });

        this.exchange.getConnection().getWorker().execute(new Runnable() {
            @Override
            public void run() {
                DelayedExecutor executor = new DelayedExecutor(exchange.getIoThread());
                final ThreadSetupAction.Handle handle = threadSetupAction.setup(ServletUpgradeListener.this.exchange);
                try {
                    //run the upgrade in the worker thread
                    instance.getInstance().init(new WebConnectionImpl(channel, ServletUpgradeListener.this.exchange.getConnection().getBufferPool(), executor));
                } finally {
                    try {
                        handle.tearDown();
                    } finally {
                        executor.openGate();
                    }
                }
            }
        });
    }

    /**
     * Executor that delays submitting tasks to the delegate until a condition is satisfied.
     */
    private static final class DelayedExecutor implements Executor {

        private final Executor delegate;
        private volatile boolean queue = true;
        private final List<Runnable> tasks = new ArrayList<Runnable>();

        private DelayedExecutor(Executor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            if (!queue) {
                delegate.execute(command);
            } else {
                synchronized (this) {
                    if (!queue) {
                        delegate.execute(command);
                    } else {
                        tasks.add(command);
                    }
                }
            }
        }

        synchronized void openGate() {
            queue = false;
            for (Runnable task : tasks) {
                delegate.execute(task);
            }
        }
    }
}
