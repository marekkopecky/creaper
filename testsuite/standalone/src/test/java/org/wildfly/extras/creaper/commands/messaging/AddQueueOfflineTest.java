package org.wildfly.extras.creaper.commands.messaging;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.wildfly.extras.creaper.XmlAssert;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineOptions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.wildfly.extras.creaper.XmlAssert.assertXmlIdentical;
import static org.junit.Assert.fail;

public class AddQueueOfflineTest {
    private static final String TEST_QUEUE_NAME = "testQueue";
    private static final String TEST_QUEUE_NAME_JNDI = "java:/jms/queue/" + TEST_QUEUE_NAME;

    private static final String SUBSYTEM_ORIGINAL = ""
            + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
            + "    <profile>\n"
            + "        <subsystem xmlns=\"urn:jboss:domain:messaging:1.4\">"
            + "            <hornetq-server>\n"
            + "                  <jms-destinations>\n"
            + "                    <jms-queue name=\"ExpiryQueue\">\n"
            + "                        <entry name=\"java:/jms/queue/ExpiryQueue\"/>\n"
            + "                    </jms-queue>\n"
            + "                    <jms-queue name=\"DLQ\">\n"
            + "                        <entry name=\"java:/jms/queue/DLQ\"/>\n"
            + "                    </jms-queue>\n"
            + "                </jms-destinations>"
            + "            </hornetq-server>\n"
            + "        </subsystem>\n"
            + "    </profile>\n"
            + "</server>";
    private static final String SUBSYSTEM_EXPECTED_TRANSFORM = ""
            + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
            + "    <profile>\n"
            + "        <subsystem xmlns=\"urn:jboss:domain:messaging:1.4\">"
            + "            <hornetq-server>\n"
            + "                  <jms-destinations>\n"
            + "                    <jms-queue name=\"ExpiryQueue\">\n"
            + "                        <entry name=\"java:/jms/queue/ExpiryQueue\"/>\n"
            + "                    </jms-queue>\n"
            + "                    <jms-queue name=\"DLQ\">\n"
            + "                        <entry name=\"java:/jms/queue/DLQ\"/>\n"
            + "                    </jms-queue>\n"
            + "                     <jms-queue name=\"" + TEST_QUEUE_NAME + "\">\n"
            + "                         <entry name=\"" + TEST_QUEUE_NAME_JNDI + "\"/>\n"
            + "                         <durable>true</durable>\n"
            + "                     </jms-queue>\n"
            + "                </jms-destinations>"
            + "            </hornetq-server>\n"
            + "        </subsystem>\n"
            + "    </profile>\n"
            + "</server>";
    private static final String SUBSYSTEM_EXPECTED_REPLACE = ""
            + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
            + "    <profile>\n"
            + "        <subsystem xmlns=\"urn:jboss:domain:messaging:1.4\">"
            + "            <hornetq-server>\n"
            + "                  <jms-destinations>\n"
            + "                    <jms-queue name=\"ExpiryQueue\">\n"
            + "                        <entry name=\"java:/jms/queue/ExpiryQueue\"/>\n"
            + "                    </jms-queue>\n"
            + "                    <jms-queue name=\"DLQ\">\n"
            + "                        <entry name=\"java:/jms/queue/DLQ\"/>\n"
            + "                        <selector>creaper</selector>\n"
            + "                        <durable>true</durable>\n"
            + "                    </jms-queue>\n"
            + "                </jms-destinations>"
            + "            </hornetq-server>\n"
            + "        </subsystem>\n"
            + "    </profile>\n"
            + "</server>";

    private static final String SUBSYTEM_ORIGINAL_ACTIVEMQ = ""
            + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
            + "    <profile>\n"
            + "        <subsystem xmlns=\"urn:jboss:domain:messaging-activemq:1.0\">\n"
            + "            <server name=\"default\">\n"
            + "                    <jms-queue name=\"ExpiryQueue\" entries=\"java:/jms/queue/ExpiryQueue\"/>\n"
            + "                <jms-queue name=\"DLQ\" entries=\"java:/jms/queue/DLQ\"/>\n"
            + "            </server>\n"
            + "        </subsystem>\n"
            + "    </profile>\n"
            + "</server>";
    private static final String SUBSYSTEM_EXPECTED_TRANSFORM_ACTIVEMQ = ""
            + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
            + "    <profile>\n"
            + "        <subsystem xmlns=\"urn:jboss:domain:messaging-activemq:1.0\">\n"
            + "            <server name=\"default\">\n"
            + "                <jms-queue name=\"ExpiryQueue\" entries=\"java:/jms/queue/ExpiryQueue\"/>\n"
            + "                <jms-queue name=\"DLQ\" entries=\"java:/jms/queue/DLQ\"/>\n"
            + "                <jms-queue name=\"testQueue\" entries=\"" + TEST_QUEUE_NAME_JNDI + "1 " + TEST_QUEUE_NAME_JNDI + "2" + "\" selector=\"aaaa\" durable=\"false\"/>"
            + "            </server>\n"
            + "        </subsystem>\n"
            + "    </profile>\n"
            + "</server>";

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void setUp() {
        // ignore whitespaces difference in "text" node
        XmlAssert.setNormalizeWhitespace(true);
    }

    @Test(expected = CommandFailedException.class)
    public void existing() throws Exception {
        File cfg = tmp.newFile("xmlTransform.xml");
        Files.write(SUBSYTEM_ORIGINAL, cfg, Charsets.UTF_8);

        OfflineManagementClient client = ManagementClient.offline(
                OfflineOptions.standalone().configurationFile(cfg).build());

        assertXmlIdentical(SUBSYTEM_ORIGINAL, Files.toString(cfg, Charsets.UTF_8));

        client.apply(new AddQueue.Builder("DLQ", "default")
                .jndiEntries(Collections.singletonList("java:/jms/queue/DLQ"))
                .durable(true)
                .selector("creaper")
                .build());

        fail("Queue DLQ already exists in configuration, exception should be thrown");
    }

    @Test
    public void overrideExisting() throws Exception {
        File cfg = tmp.newFile("xmlTransform.xml");
        Files.write(SUBSYTEM_ORIGINAL, cfg, Charsets.UTF_8);

        OfflineManagementClient client = ManagementClient.offline(
                OfflineOptions.standalone().configurationFile(cfg).build());

        assertXmlIdentical(SUBSYTEM_ORIGINAL, Files.toString(cfg, Charsets.UTF_8));

        client.apply(new AddQueue.Builder("DLQ")
                .jndiEntries(Collections.singletonList("java:/jms/queue/DLQ"))
                .durable(true)
                .selector("creaper")
                .replaceExisting()
                .build());

        assertXmlIdentical(SUBSYSTEM_EXPECTED_REPLACE, Files.toString(cfg, Charsets.UTF_8));
    }

    @Test
    public void transform() throws Exception {
        File cfg = tmp.newFile("xmlTransform.xml");
        Files.write(SUBSYTEM_ORIGINAL, cfg, Charsets.UTF_8);

        OfflineManagementClient client = ManagementClient.offline(
                OfflineOptions.standalone().configurationFile(cfg).build());

        assertXmlIdentical(SUBSYTEM_ORIGINAL, Files.toString(cfg, Charsets.UTF_8));

        client.apply(new AddQueue.Builder(TEST_QUEUE_NAME)
                .jndiEntries(Collections.singletonList(TEST_QUEUE_NAME_JNDI))
                .durable(true)
                .build());

        assertXmlIdentical(SUBSYSTEM_EXPECTED_TRANSFORM, Files.toString(cfg, Charsets.UTF_8));
    }

    @Test
    public void transformActiveMQ() throws Exception {
        File cfg = tmp.newFile("xmlTransform.xml");
        Files.write(SUBSYTEM_ORIGINAL_ACTIVEMQ, cfg, Charsets.UTF_8);

        OfflineManagementClient client = ManagementClient.offline(
                OfflineOptions.standalone().configurationFile(cfg).build());

        assertXmlIdentical(SUBSYTEM_ORIGINAL_ACTIVEMQ, Files.toString(cfg, Charsets.UTF_8));

        client.apply(new AddQueue.Builder(TEST_QUEUE_NAME)
                .jndiEntries(Arrays.asList(TEST_QUEUE_NAME_JNDI + "1", TEST_QUEUE_NAME_JNDI + "2"))
                .selector("aaaa")
                .durable(false)
                .build()
        );

        assertXmlIdentical(SUBSYSTEM_EXPECTED_TRANSFORM_ACTIVEMQ, Files.toString(cfg, Charsets.UTF_8));
    }
}
