/*
 * Copyright (C) 2005-2013 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.manydesigns.portofino.interceptors;

import com.manydesigns.elements.ElementsThreadLocals;
import com.manydesigns.portofino.RequestAttributes;
import com.manydesigns.portofino.application.Application;
import com.manydesigns.portofino.dispatcher.Dispatch;
import com.manydesigns.portofino.dispatcher.DispatcherUtil;
import com.manydesigns.portofino.logic.SecurityLogic;
import com.manydesigns.portofino.pages.Permissions;
import com.manydesigns.portofino.shiro.SecurityUtilsBean;
import com.manydesigns.portofino.stripes.ForbiddenAccessResolution;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.ExecutionContext;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.Intercepts;
import net.sourceforge.stripes.controller.LifecycleStage;
import ognl.OgnlContext;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
@Intercepts(LifecycleStage.CustomValidation)
public class
        SecurityInterceptor implements Interceptor {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    public final static Logger logger =
            LoggerFactory.getLogger(SecurityInterceptor.class);

    public Resolution intercept(ExecutionContext context) throws Exception {
        logger.debug("Retrieving Stripes objects");
        ActionBeanContext actionContext = context.getActionBeanContext();
        ActionBean actionBean = context.getActionBean();
        Method handler = context.getHandler();

        logger.debug("Retrieving Servlet API objects");
        HttpServletRequest request = actionContext.getRequest();

        logger.debug("Retrieving Portofino application");
        Application application =
                (Application) request.getAttribute(
                        RequestAttributes.APPLICATION);

        logger.debug("Retrieving user");
        String userId = null;
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            userId = subject.getPrincipal().toString();
            logger.debug("Retrieved userId={}", userId);
        } else {
            logger.debug("No user found");
        }

        logger.debug("Publishing securityUtils in OGNL context");
        OgnlContext ognlContext = ElementsThreadLocals.getOgnlContext();
        ognlContext.put("securityUtils", new SecurityUtilsBean());

        logger.debug("Setting up logging MDC");
        MDC.clear();
        if(userId != null) { //Issue #755
            MDC.put("userId", userId);
        }

        if (!SecurityLogic.satisfiesRequiresAdministrator(request, actionBean, handler)) {
            return new ForbiddenAccessResolution();
        }

        logger.debug("Checking page permissions");
        boolean isNotAdmin = !SecurityLogic.isAdministrator(request);
        if (isNotAdmin) {
            Permissions permissions;
            Dispatch dispatch = DispatcherUtil.getDispatch(request);
            String resource;
            boolean allowed;
            if(dispatch != null) {
                resource = dispatch.getLastPageInstance().getPath();
                allowed = SecurityLogic.hasPermissions(dispatch, subject, handler);
            } else {
                resource = request.getRequestURI();
                permissions = new Permissions();
                allowed = SecurityLogic.hasPermissions
                        (application, permissions, subject, handler, actionBean.getClass());
            }
            if(!allowed) {
                logger.info("User {} is not allowed for {}", userId, resource);
                return new ForbiddenAccessResolution();
            }
        }

        logger.debug("Security check passed.");
        return context.proceed();
    }

}
