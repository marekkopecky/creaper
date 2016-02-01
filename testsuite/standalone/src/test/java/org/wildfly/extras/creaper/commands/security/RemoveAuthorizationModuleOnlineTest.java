package org.wildfly.extras.creaper.commands.security;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;
import org.junit.Ignore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class RemoveAuthorizationModuleOnlineTest {

    private OnlineManagementClient client;
    private Operations ops;
    private Administration administration;

    private static final String AUTHORIZATION_MODULE1_CODE = "Delegating";
    private static final String AUTHORIZATION_MODULE2_CODE = "Web";
    private static final String AUTHORIZATION_MODULE2_NAME = "NamedWeb";

    private static final String TEST_SECURITY_DOMAIN_NAME = "creaperSecDomain";
    private static final Address TEST_SECURITY_DOMAIN_ADDRESS
            = Address.subsystem("security").and("security-domain", TEST_SECURITY_DOMAIN_NAME);
    private static final Address TEST_AUTHORIZATION_ADDRESS
            = TEST_SECURITY_DOMAIN_ADDRESS.and("authorization", "classic");
    private static final Address TEST_AUTHORIZATION_MODULE1_ADDRESS
            = TEST_AUTHORIZATION_ADDRESS.and("policy-module", AUTHORIZATION_MODULE1_CODE);
    private static final Address TEST_AUTHORIZATION_MODULE2_ADDRESS
            = TEST_AUTHORIZATION_ADDRESS.and("policy-module", AUTHORIZATION_MODULE2_NAME);

    private static final String TEST_NON_EXIST_SECURITY_DOMAIN = "nonExistSecurityDomain";

    private static final AddAuthorizationModule ADD_AUTHORIZATION_MODULE_1
            = new AddAuthorizationModule.Builder(AUTHORIZATION_MODULE1_CODE)
            .securityDomainName(TEST_SECURITY_DOMAIN_NAME)
            .flag("required")
            .addModuleOption("delegateMap", "delegateMapValue")
            .build();

    private static final AddAuthorizationModule ADD_AUTHORIZATION_MODULE_2
            = new AddAuthorizationModule.Builder(AUTHORIZATION_MODULE2_CODE,
                    AUTHORIZATION_MODULE2_NAME)
            .securityDomainName(TEST_SECURITY_DOMAIN_NAME)
            .flag("required")
            .addModuleOption("delegateMap", "delegateMapValue")
            .build();

    @Before
    public void connect() throws IOException, CommandFailedException, OperationException {
        client = ManagementClient.online(OnlineOptions.standalone().localDefault().build());
        ops = new Operations(client);
        administration = new Administration(client);

        AddSecurityDomain addSecurityDomain = new AddSecurityDomain.Builder(TEST_SECURITY_DOMAIN_NAME).build();
        client.apply(addSecurityDomain);
        assertTrue("The security domain should be created", ops.exists(TEST_SECURITY_DOMAIN_ADDRESS));
    }

    @After
    public void cleanup() throws IOException, CliException, OperationException, TimeoutException, InterruptedException {
        try {
            ops.removeIfExists(TEST_SECURITY_DOMAIN_ADDRESS);
            administration.reloadIfRequired();
        } finally {
            client.close();
        }
    }

    @Test
    public void removeOneOfMoreAuthorizationModules() throws Exception {
        client.apply(ADD_AUTHORIZATION_MODULE_1);
        assertTrue("The authorization module should be created", ops.exists(TEST_AUTHORIZATION_MODULE1_ADDRESS));
        client.apply(ADD_AUTHORIZATION_MODULE_2);
        assertTrue("The authorization module should be created", ops.exists(TEST_AUTHORIZATION_MODULE2_ADDRESS));

        client.apply(new RemoveAuthorizationModule(TEST_SECURITY_DOMAIN_NAME, AUTHORIZATION_MODULE1_CODE));
        assertFalse("The authorization module should be removed", ops.exists(TEST_AUTHORIZATION_MODULE1_ADDRESS));
    }

    @Test
    public void removeLastAuthorizationModule() throws Exception {
        client.apply(ADD_AUTHORIZATION_MODULE_1);
        assertTrue("The authorization module should be created", ops.exists(TEST_AUTHORIZATION_MODULE1_ADDRESS));

        client.apply(new RemoveAuthorizationModule(TEST_SECURITY_DOMAIN_NAME, AUTHORIZATION_MODULE1_CODE));
        assertFalse("The authorization module should be removed", ops.exists(TEST_AUTHORIZATION_MODULE1_ADDRESS));
    }

    @Test(expected = CommandFailedException.class)
    @Ignore("https://issues.jboss.org/browse/JBEAP-3112")
    public void removeNonExistingAuthorizationModule() throws Exception {
        client.apply(ADD_AUTHORIZATION_MODULE_2);
        assertTrue("The authorization module should be created", ops.exists(TEST_AUTHORIZATION_MODULE2_ADDRESS));

        client.apply(new RemoveAuthorizationModule(TEST_SECURITY_DOMAIN_NAME, AUTHORIZATION_MODULE1_CODE));
        fail("Authorization module UsersRoles does not exist in configuration, exception should be thrown");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullNameAuthorizationModule() throws Exception {
        client.apply(new RemoveAuthorizationModule(TEST_SECURITY_DOMAIN_NAME, null));
        fail("Creating command with null authorization module name should throw exception");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeEmptyNameAuthorizationModule() throws Exception {
        client.apply(new RemoveAuthorizationModule(TEST_SECURITY_DOMAIN_NAME, ""));
        fail("Creating command with empty authorization module name should throw exception");
    }

    @Test(expected = CommandFailedException.class)
    public void removeOnNonExistingSecurityDomain() throws Exception {
        client.apply(ADD_AUTHORIZATION_MODULE_1);
        assertTrue("The authorization module should be created", ops.exists(TEST_AUTHORIZATION_MODULE1_ADDRESS));

        client.apply(new RemoveAuthorizationModule(TEST_NON_EXIST_SECURITY_DOMAIN, AUTHORIZATION_MODULE1_CODE));
        fail("Using non-existing security domain should throw exception");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeOnNullNameSecurityDomain() throws Exception {
        client.apply(new RemoveAuthorizationModule(null, AUTHORIZATION_MODULE1_CODE));
        fail("Creating command with null security domain name should throw exception");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeOnEmptyNameSecurityDomain() throws Exception {
        client.apply(new RemoveAuthorizationModule("", AUTHORIZATION_MODULE1_CODE));
        fail("Creating command with empty security domain name should throw exception");
    }

    @Test(expected = CommandFailedException.class)
    @Ignore("https://issues.jboss.org/browse/JBEAP-3112")
    public void doNotRemoveNamedAuthorizationModuleByCodeReference() throws Exception {
        client.apply(ADD_AUTHORIZATION_MODULE_2);
        assertTrue("The authorization module should be created", ops.exists(TEST_AUTHORIZATION_MODULE2_ADDRESS));

        client.apply(new RemoveAuthorizationModule(TEST_SECURITY_DOMAIN_NAME, AUTHORIZATION_MODULE2_CODE));
        fail("Removing named authorization module based on code should be unable, exception should be thrown");
    }
}
