/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.ScheduledPollEndpoint;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an SSH endpoint.
 */
public class SshEndpoint extends ScheduledPollEndpoint {
    protected final transient Logger log = LoggerFactory.getLogger(getClass());

    private SshClient client;
    private SshConfiguration sshConfiguration;

    public SshEndpoint() {
    }

    public SshEndpoint(String uri, SshComponent component) {
        super(uri, component);
    }

    public SshEndpoint(String uri, SshComponent component, SshConfiguration configuration) {
        super(uri, component);
        this.sshConfiguration = configuration;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new SshProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        SshConsumer consumer = new SshConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public boolean isSingleton() {
        // SshClient is not thread-safe to be shared
        return false;
    }

    public byte[] sendExecCommand(String command) throws Exception {
        byte[] result = null;

        if (getConfiguration() == null) {
            throw new IllegalStateException("Configuration must be set");
        }

        ConnectFuture connectFuture = client.connect(getHost(), getPort());

        // Wait getTimeout milliseconds for connect operation to complete
        connectFuture.await(getTimeout());

        if (!connectFuture.isDone() || !connectFuture.isConnected()) {
            final String msg = "Failed to connect to " + getHost() + ":" + getPort() + " within timeout " + getTimeout() + "ms";
            log.debug(msg);
            throw new RuntimeCamelException(msg);
        }

        log.debug("Connected to {}:{}", getHost(), getPort());

        AuthFuture authResult;
        ClientSession session = connectFuture.getSession();

        KeyPairProvider keyPairProvider;
        final String certFilename = getCertFilename();
        if (certFilename != null) {
            log.debug("Attempting to authenticate using FileKey '{}'...", certFilename);
            keyPairProvider = new FileKeyPairProvider(new String[]{certFilename});
        } else {
            keyPairProvider = getKeyPairProvider();
        }

        if (keyPairProvider != null) {
            log.debug("Attempting to authenticate username '{}' using Key...", getUsername());
            KeyPair pair = keyPairProvider.loadKey(getKeyType());
            authResult = session.authPublicKey(getUsername(), pair);
        } else {
            log.debug("Attempting to authenticate username '{}' using Password...", getUsername());
            authResult = session.authPassword(getUsername(), getPassword());
        }

        authResult.await(getTimeout());

        if (!authResult.isDone() || authResult.isFailure()) {
            log.debug("Failed to authenticate");
            throw new RuntimeCamelException("Failed to authenticate username " + getUsername());
        }

        ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_EXEC, command);

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{0});
        channel.setIn(in);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        channel.setOut(out);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        channel.setErr(err);

        OpenFuture openFuture = channel.open();
        openFuture.await(getTimeout());
        if (openFuture.isOpened()) {
            channel.waitFor(ClientChannel.CLOSED, 0);
            result = out.toByteArray();
        }

        return result;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        client = SshClient.setUpDefaultClient();
        client.start();
    }

    @Override
    protected void doStop() throws Exception {
        if (client != null) {
            client.stop();
            client = null;
        }

        super.doStop();
    }

    public SshConfiguration getConfiguration() {
        return sshConfiguration;
    }

    public void setConfiguration(SshConfiguration configuration) {
        this.sshConfiguration = configuration;
    }

    public String getHost() {
        return getConfiguration().getHost();
    }

    public void setHost(String host) {
        getConfiguration().setHost(host);
    }

    public int getPort() {
        return getConfiguration().getPort();
    }

    public void setPort(int port) {
        getConfiguration().setPort(port);
    }

    public String getUsername() {
        return getConfiguration().getUsername();
    }

    public void setUsername(String username) {
        getConfiguration().setUsername(username);
    }

    public String getPassword() {
        return getConfiguration().getPassword();
    }

    public void setPassword(String password) {
        getConfiguration().setPassword(password);
    }

    public String getPollCommand() {
        return getConfiguration().getPollCommand();
    }

    public void setPollCommand(String pollCommand) {
        getConfiguration().setPollCommand(pollCommand);
    }

    public KeyPairProvider getKeyPairProvider() {
        return getConfiguration().getKeyPairProvider();
    }

    public void setKeyPairProvider(KeyPairProvider keyPairProvider) {
        getConfiguration().setKeyPairProvider(keyPairProvider);
    }

    public String getKeyType() {
        return getConfiguration().getKeyType();
    }

    public void setKeyType(String keyType) {
        getConfiguration().setKeyType(keyType);
    }

    public long getTimeout() {
        return getConfiguration().getTimeout();
    }

    public void setTimeout(long timeout) {
        getConfiguration().setTimeout(timeout);
    }
    
    public String getCertFilename() {
        return getConfiguration().getCertFilename();
    }
    
    public void setCertFilename(String certFilename) {
        getConfiguration().setCertFilename(certFilename);
    }
}
