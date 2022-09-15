package org.oresat.uniclogs;

import java.io.IOException;

import org.yamcs.AbstractYamcsService;
import org.yamcs.InitException;
import org.yamcs.YConfiguration;
import org.yamcs.utils.ByteArrayUtils;
import org.yamcs.yarch.Bucket;
import org.yamcs.yarch.YarchDatabase;
import org.yamcs.yarch.YarchDatabaseInstance;

public class UniclogsEnvironment extends AbstractYamcsService {
    static String sequenceNumberId = "seqNum";
    static String hmacKeyId = "hmacKey";


    byte[] hmacKey;
    Integer seqNum;
    Bucket db;

    @Override
    protected void doStart() {
        try {
            this.hmacKey = this.loadHmacKey();
            this.seqNum = this.loadSeqNum();
            this.log.info("Loaded Sequence Number: " + this.seqNum);
        } catch (IOException e) {
            this.log.error(e.getMessage());
        }
        this.notifyStarted();
        
    }

    @Override
    protected void doStop() {
        try {
            this.saveSeqNum(this.seqNum);
            this.saveHmacKey(this.hmacKey);
        } catch (IOException e) {
            this.log.error(e.getMessage());
        }
        this.notifyStopped();
    }

    public Integer getSeqNum() {
        Integer num = this.seqNum;
        this.seqNum++;
        return num;
    }

    public byte[] getHmacKey() {
        return this.hmacKey;
    }

    @Override
    public void init(String yamcsInstance, String serviceName, YConfiguration config) throws InitException {
        super.init(yamcsInstance, serviceName, config);
        YarchDatabaseInstance instanceDb = YarchDatabase.getInstance(yamcsInstance);
        try {
            this.db = instanceDb.getBucket("env");
            if (this.db == null) {
                this.log.info(String.format("Env not found for %s, creating new env...", yamcsInstance));
                this.db = instanceDb.createBucket("env");
            }
        } catch (IOException e) {
            throw new InitException(e.getMessage());
        }
    }

    private Integer loadSeqNum() throws IOException {
        byte[] numBytes = db.getObject(sequenceNumberId);
        if (numBytes == null) {
            this.log.info(String.format("Sequence Number not found for %s, Sequence Number set to 1", yamcsInstance));
            this.saveSeqNum(1);
            return 1;
        }
        return ByteArrayUtils.decodeInt(db.getObject(sequenceNumberId), 0);
    }

    private byte[] loadHmacKey() throws IOException {
        byte[] testKey = {0x00, 0x01};
        byte[] hmac = this.db.getObject(hmacKeyId);
        if (hmac == null) {
            this.log.info(String.format("Hmac Key not found for %s, Hmac Key set to %s", yamcsInstance, testKey));
            this.saveHmacKey(testKey);
            return testKey;
        }
        return hmac;
    }

    private void saveSeqNum(Integer seq) throws IOException {
        db.putObject(sequenceNumberId, "Integer", null, ByteArrayUtils.encodeInt(seq));
    }

    private void saveHmacKey(byte[] hmacKey) throws IOException {
        db.putObject(hmacKeyId, "bytes", null, hmacKey);
    }
}
