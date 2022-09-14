package org.oresat.uniclogs;

import java.util.Date;
import org.yamcs.TmPacket;
import org.yamcs.YConfiguration;
import org.yamcs.tctm.AbstractPacketPreprocessor;

public class EdlPacketPreprocessor extends AbstractPacketPreprocessor {

    public EdlPacketPreprocessor(String yamcsInstance, YConfiguration config) {
        super(yamcsInstance, config);
    }

    public EdlPacketPreprocessor(String yamcsInstance) {
        super(yamcsInstance, YConfiguration.emptyConfig());
    }

    @Override
    public TmPacket process(TmPacket tmPacket) {
        EDLPacket packet = new EDLPacket(tmPacket);
        tmPacket.setInvalid(!packet.containsValidCrc());
        tmPacket.setSequenceCount(packet.getSeqNum());
        tmPacket.setGenerationTime(new Date().getTime());
        tmPacket.setLocalGenTimeFlag();
        return tmPacket;
    }
}