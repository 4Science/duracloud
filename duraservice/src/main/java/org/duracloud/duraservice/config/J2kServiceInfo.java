package org.duracloud.duraservice.config;

import org.duracloud.duraservice.mgmt.ServiceConfigUtil;
import org.duracloud.serviceconfig.ServiceInfo;
import org.duracloud.serviceconfig.SystemConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Woods
 *         Date: Aug 3, 2010
 */
public class J2kServiceInfo extends AbstractServiceInfo {
    @Override
    public ServiceInfo getServiceXml(int index, String version) {
        ServiceInfo j2kService = new ServiceInfo();
        j2kService.setId(index);
        j2kService.setContentId("j2kservice-" + version + ".zip");
        String desc =
            "The J2K service deploys an instance of the Adore Djatoka web " +
            "application which provides for serving and viewing JPEG2000 " +
            "images.";
        j2kService.setDescription(desc);
        j2kService.setDisplayName("JPEG 2000 Image Viewer Service");
        j2kService.setUserConfigVersion("1.0");
        j2kService.setServiceVersion(version);
        j2kService.setMaxDeploymentsAllowed(1);

        // System Configs
        List<SystemConfig> systemConfig = new ArrayList<SystemConfig>();

        SystemConfig host = new SystemConfig("host",
                                             ServiceConfigUtil.STORE_HOST_VAR,
                                             "localhost");
        SystemConfig port = new SystemConfig("port",
                                             ServiceConfigUtil.STORE_PORT_VAR,
                                             "8080");
        SystemConfig username = new SystemConfig("username",
                                                 ServiceConfigUtil.STORE_USER_VAR,
                                                 "no-username");
        SystemConfig password = new SystemConfig("password",
                                                 ServiceConfigUtil.STORE_PWORD_VAR,
                                                 "no-password");

        systemConfig.add(host);
        systemConfig.add(port);
        systemConfig.add(username);
        systemConfig.add(password);

        j2kService.setSystemConfigs(systemConfig);

        j2kService.setDeploymentOptions(getSimpleDeploymentOptions());

        return j2kService;
    }
}
