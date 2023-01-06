package org.wildfly.extras.creaper.commands.infinispan.cache;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.ServerVersion;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class AddInvalidationCacheOnlineTest {
    private OnlineManagementClient client;
    private Operations ops;

    private static final String TEST_CACHE_NAME = UUID.randomUUID().toString();

    private static final Address TEST_CACHE_ADDRESS = Address.subsystem("infinispan")
            .and("cache-container", "hibernate")
            .and("invalidation-cache", TEST_CACHE_NAME);

    @BeforeClass
    public static void checkServerVersionIsSupported() throws Exception {
        // check version is supported
        ServerVersion serverVersion
                = ManagementClient.online(OnlineOptions.standalone().localDefault().build()).version();
        Assume.assumeFalse("The command is not compatible with WildFly 27 and above,"
                        + " see https://github.com/wildfly-extras/creaper/issues/218.",
                serverVersion.greaterThanOrEqualTo(ServerVersion.VERSION_20_0_0));
    }

    @Before
    public void connect() throws Exception {
        client = ManagementClient.online(OnlineOptions.standalone().localDefault().build());
        ops = new Operations(client);
    }

    @After
    public void after() throws CommandFailedException, IOException, OperationException {
        client.apply(new RemoveCache("hibernate", CacheType.INVALIDATION_CACHE, TEST_CACHE_NAME));
        client.close();
    }

    @Test
    public void addCacheWithRequiredArgsOnly() throws CommandFailedException, IOException {
        AddInvalidationCache cmd = new AddInvalidationCache.Builder(TEST_CACHE_NAME)
                .cacheContainer("hibernate")
                .mode(CacheMode.SYNC)
                .build();
        client.apply(cmd);

        ModelNodeResult resource = ops.readResource(TEST_CACHE_ADDRESS);

        assertTrue(resource.isSuccess());
        assertEquals("SYNC", ops.readAttribute(TEST_CACHE_ADDRESS, "mode").stringValue());
    }

    @Test
    public void addCacheWithMoreArgs() throws CommandFailedException, IOException {
        AddInvalidationCache cmd = new AddInvalidationCache.Builder(TEST_CACHE_NAME)
                .cacheContainer("hibernate")
                .mode(CacheMode.SYNC)
                .asyncMarshalling(true)
                .queueFlushInterval(1234L)
                .remoteTimeout(4321L)
                .statisticsEnabled(false)
                .build();
        client.apply(cmd);

        ModelNodeResult resource = ops.readResource(TEST_CACHE_ADDRESS);

        assertTrue(resource.isSuccess());
        assertEquals(CacheMode.SYNC.getMode(), ops.readAttribute(TEST_CACHE_ADDRESS, "mode").stringValue());
        assertEquals(true, ops.readAttribute(TEST_CACHE_ADDRESS, "async-marshalling").booleanValue());
        assertEquals(1234L, ops.readAttribute(TEST_CACHE_ADDRESS, "queue-flush-interval").longValue());
        assertEquals(4321L, ops.readAttribute(TEST_CACHE_ADDRESS, "remote-timeout").longValue());
        assertEquals(false, ops.readAttribute(TEST_CACHE_ADDRESS, "statistics-enabled").booleanValue());
    }
}
