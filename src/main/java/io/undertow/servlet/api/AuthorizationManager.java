package io.undertow.servlet.api;

import io.undertow.security.idm.Account;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Authorization manager. The servlet implementation delegates all authorization checks to this interface.
 *
 * @author Stuart Douglas
 */
public interface AuthorizationManager {

    /**
     * Tests if a user is in a given role
     * @param roleName The role name
     * @param account The user account
     * @param servletInfo The servlet info for the target servlet
     * @param request The servlet request
     * @param deployment The deployment
     * @return true if the user is in the role
     */
    boolean isUserInRole(String roleName, final Account account, final ServletInfo servletInfo, final HttpServletRequest request, Deployment deployment);

    /**
     * Tests if a user can access a given resource
     *
     * @param mappedConstraints The constraints
     * @param account The users account
     * @param servletInfo The servlet info for the target servlet
     * @param request The servlet request
     * @param deployment The deployment
     * @return true if the user can access the resource
     */
    boolean canAccessResource(List<SingleConstraintMatch> mappedConstraints, final Account account, final ServletInfo servletInfo, final HttpServletRequest request, Deployment deployment);

    /**
     * Determines the transport guarantee type
     *
     * @param currentConnectionGuarantee The current connections transport guarantee type
     * @param configuredRequiredGuarantee The transport guarantee type specified in the deployment descriptor/annotations
     * @param request The request
     * @return The transport guarantee type
     */
    TransportGuaranteeType transportGuarantee(TransportGuaranteeType currentConnectionGuarantee, TransportGuaranteeType configuredRequiredGuarantee, final HttpServletRequest request);

}
