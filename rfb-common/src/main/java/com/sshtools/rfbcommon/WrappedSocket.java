package com.sshtools.rfbcommon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOptions;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class WrappedSocket extends Socket {
    final static Logger LOG = Logger.getLogger(WrappedSocket.class.getName());

    public WrappedSocket(InputStream in, OutputStream out) throws SocketException {
        super(new WrappedSocketImpl(in, out));
    }

    @Override
    public InetAddress getInetAddress() {
        // TODO Auto-generated method stub
        return super.getInetAddress();
    }

    static class WrappedSocketImpl extends SocketImpl {
        
        private InputStream in;
        private OutputStream out;

        WrappedSocketImpl(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }
        
        @Override
        protected InetAddress getInetAddress() {
            // Will throw NPE during SSL server handshake if there is no address
            // TODO maybe need a way to provide this
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Currently need to be able to resolve localhost for wrapped socket to work.", e);
            }
        }

        public void setOption(int optID, Object value) throws SocketException {
            LOG.warning(String.format("Unsupported option %d = %s", optID, String.valueOf(value)));
        }

        public Object getOption(int optID) throws SocketException {
            // TODO make RFBTransport support this somehow?
            switch (optID) {
            case SocketOptions.TCP_NODELAY:
                return Boolean.FALSE;
            case SocketOptions.SO_LINGER:
                return Boolean.FALSE;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported option ID " + optID);
            }
        }

        @Override
        protected void sendUrgentData(int data) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void listen(int backlog) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            return out;
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return in;
        }

        @Override
        protected void create(boolean stream) throws IOException {
        }

        @Override
        protected void connect(SocketAddress address, int timeout) throws IOException {
            LOG.info("Connect " + address + ",timeout=" + timeout);
        }

        @Override
        protected void connect(InetAddress address, int port) throws IOException {
            LOG.info("Connect " + address + ",port=" + port);
        }

        @Override
        protected void connect(String host, int port) throws IOException {
            LOG.info("Connect " + host + ",port=" + port);
        }

        @Override
        protected void close() throws IOException {
        }

        @Override
        protected void bind(InetAddress host, int port) throws IOException {
            LOG.info("Bind " + host + ",port=" + port);
        }

        @Override
        protected int available() throws IOException {
            return in.available();
        }

        @Override
        protected void accept(SocketImpl s) throws IOException {
            LOG.info("Accept " + s);
        }
    }
}
