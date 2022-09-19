package org.oresat.uniclogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.yamcs.AbstractYamcsService;
import org.yamcs.InitException;
import org.yamcs.YConfiguration;
import org.yamcs.YamcsServer;
import org.yamcs.utils.ByteArrayUtils;
import org.yamcs.yarch.Bucket;
import org.yamcs.yarch.YarchDatabase;
import org.yamcs.yarch.YarchDatabaseInstance;

public class UniclogsEnvironment extends AbstractYamcsService {
    static final String sequenceNumberId = "seqNum";
    static final String hmacKeyId = "hmacKey";
    String hmacFilePath;
    byte[] hmacKey;
    Integer seqNum;
    Bucket db;


    private byte[] loadHmacFromFile(String filepath) throws IOException {
        try (FileReader fr = new FileReader(filepath);
            BufferedReader br = new BufferedReader(fr)) {
                return br.readLine().getBytes();
            }
    }

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
        this.hmacFilePath = YamcsServer.getServer().getDataDirectory() + "/" + config.getString("hmacFileName");
        log.info(this.hmacFilePath);
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
        byte[] hmac = this.db.getObject(hmacKeyId);
        if (hmac == null) {
            this.log.info(String.format("Hmac Key not in %s. Loading from file: %s", yamcsInstance, this.hmacFilePath));
            hmac = this.loadHmacFromFile(this.hmacFilePath);
            this.saveHmacKey(hmac);
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
