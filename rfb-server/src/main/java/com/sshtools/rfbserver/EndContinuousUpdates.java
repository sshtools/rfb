package com.sshtools.rfbserver;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.protocol.Reply;

public class EndContinuousUpdates extends Reply<Void> {

    public EndContinuousUpdates() {
        super(RFBConstants.SMSG_END_CONTINUOUS_UPDATES);
    }

    @Override
    public void write(ProtocolWriter dout) throws IOException {
    }

}
