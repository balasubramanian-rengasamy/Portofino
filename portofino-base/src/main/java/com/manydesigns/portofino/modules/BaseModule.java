/*
* Copyright (C) 2005-2013 ManyDesigns srl.  All rights reserved.
* http://www.manydesigns.com/
*
* Unless you have purchased a commercial license agreement from ManyDesigns srl,
* the following license terms apply:
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License version 3 as published by
* the Free Software Foundation.
*
* There are special exceptions to the terms and conditions of the GPL
* as it is applied to this software. View the full text of the
* exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
* software distribution.
*
* This program is distributed WITHOUT ANY WARRANTY; and without the
* implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
* or write to:
* Free Software Foundation, Inc.,
* 59 Temple Place - Suite 330,
* Boston, MA  02111-1307  USA
*
*/

package com.manydesigns.portofino.modules;

import com.manydesigns.portofino.BaseProperties;
import com.manydesigns.portofino.actions.admin.modules.ModulesAction;
import com.manydesigns.portofino.actions.admin.servletcontext.ServletContextAction;
import com.manydesigns.portofino.di.Inject;
import com.manydesigns.portofino.files.TempFileService;
import com.manydesigns.portofino.menu.MenuBuilder;
import com.manydesigns.portofino.menu.SimpleMenuAppender;
import ognl.OgnlRuntime;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class BaseModule implements Module {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    public static final Logger logger = LoggerFactory.getLogger(BaseModule.class);

    protected ModuleStatus status = ModuleStatus.CREATED;

    //**************************************************************************
    // Constants
    //**************************************************************************

    public final static String SERVLET_CONTEXT = "com.manydesigns.portofino.servletContext";
    public final static String PORTOFINO_CONFIGURATION = "portofinoConfiguration";
    public final static String APPLICATION_DIRECTORY = "com.manydesigns.portofino.application.directory";
    public final static String RESOURCE_BUNDLE_MANAGER = "com.manydesigns.portofino.resourceBundleManager";
    public final static String ELEMENTS_CONFIGURATION = "com.manydesigns.portofino.elementsConfiguration";
    public final static String SERVER_INFO = "com.manydesigns.portofino.serverInfo";
    public final static String APP_LISTENERS = "com.manydesigns.portofino.application.listeners";
    public final static String CLASS_LOADER = "com.manydesigns.portofino.application.classLoader";
    public final static String MODULE_REGISTRY = "com.manydesigns.portofino.modules.ModuleRegistry";
    public final static String ADMIN_MENU = "com.manydesigns.portofino.menu.Menu.admin";

    //**************************************************************************
    // Injected objects
    //**************************************************************************

    @Inject(PORTOFINO_CONFIGURATION)
    public Configuration configuration;

    @Inject(ADMIN_MENU)
    public MenuBuilder adminMenu;

    //**************************************************************************
    // Module implementation
    //**************************************************************************

    @Override
    public String getModuleVersion() {
        return ModuleRegistry.getPortofinoVersion();
    }

    @Override
    public int getMigrationVersion() {
        return 1;
    }

    @Override
    public double getPriority() {
        return 0;
    }

    @Override
    public String getId() {
        return "base";
    }

    @Override
    public String getName() {
        return "Base";
    }

    @Override
    public int install() {
        return getMigrationVersion();
    }

    @Override
    public void init() {
        logger.debug("Setting up temporary file service");
        String tempFileServiceClass = configuration.getString(BaseProperties.TEMP_FILE_SERVICE_CLASS);
        if(tempFileServiceClass != null) {
            try {
                TempFileService.setInstance((TempFileService) Class.forName(tempFileServiceClass).newInstance());
            } catch (Exception e) {
                logger.error("Could not set up temp file service", e);
                throw new Error(e);
            }
        }

        //Disabilitazione security manager per funzionare su GAE. Il security manager permette di valutare
        //in sicurezza espressioni OGNL provenienti da fonti non sicure, configurando i necessari permessi
        //(invoke.<declaring-class>.<method-name>). In Portofino non permettiamo agli utenti finali di valutare
        //espressioni OGNL arbitrarie, pertanto il security manager può essere disabilitato in sicurezza.
        logger.info("Disabling OGNL security manager");
        OgnlRuntime.setSecurityManager(null);

        SimpleMenuAppender group = SimpleMenuAppender.group("configuration", null, "Configuration", 1.0);
        adminMenu.menuAppenders.add(group);

        SimpleMenuAppender link = SimpleMenuAppender.link(
                "configuration", "modules", null, "Modules", ModulesAction.URL_BINDING, 1.0);
        adminMenu.menuAppenders.add(link);

        link = SimpleMenuAppender.link(
                "configuration", "servlet-context", null, "Servlet Context", ServletContextAction.URL_BINDING, 2.0);
        adminMenu.menuAppenders.add(link);

        status = ModuleStatus.ACTIVE;
    }

    @Override
    public void destroy() {
        status = ModuleStatus.DESTROYED;
    }

    @Override
    public ModuleStatus getStatus() {
        return status;
    }

}
