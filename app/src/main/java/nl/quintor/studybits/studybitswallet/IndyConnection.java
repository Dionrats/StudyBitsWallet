package nl.quintor.studybits.studybitswallet;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.IndyJava;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.wallet.Wallet;

import java.io.Closeable;
import java.util.concurrent.ExecutionException;
import nl.quintor.studybits.indy.wrapper.IndyPool;
import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.indy.wrapper.message.IndyMessageTypes;
import nl.quintor.studybits.indy.wrapper.message.MessageEnvelopeCodec;
import nl.quintor.studybits.studybitswallet.messages.StudyBitsMessageTypes;

public class IndyConnection implements Closeable {

    private static IndyConfiguration config;
    private static IndyConnection instance;


    private IndyPool poolInstance;
    private IndyWallet walletInstance;
    private MessageEnvelopeCodec codecInstance;

    private IndyConnection(){
        try {
            this.poolInstance = new IndyPool(config.getPoolName());
            this.walletInstance = IndyWallet.open(poolInstance, config.getWalletName(), config.getStudentSeed(), config.getStudentDiD());
            this.codecInstance = new MessageEnvelopeCodec(walletInstance);

            IndyMessageTypes.init();
            StudyBitsMessageTypes.init();
        } catch (IndyException | ExecutionException | InterruptedException | JsonProcessingException e) {
            Log.e("STUDYBITS", "Exception while creating indyConnection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Closes (if it exists) the current connection and opens a new one with the current configuration.
     */
    private static void refresh() {
        if (instance != null) instance.close();
        instance = new IndyConnection();
    }

    /**
     * Checks if a connection exists, creates one if it doesn't.
     * Checks if the current connection is still valid.
     * If not it wil refresh the connection instance.
     * @return the (valid) connection instance
     */
    public static IndyConnection getInstance() {
        if (instance == null) return instance = new IndyConnection();
        try {
            // Check if pool can be reached and wallet can be read from.
            Did.keyForDid(instance.getPool().getPool(), instance.getWallet().getWallet(), instance.getWallet().getMainDid());
        } catch (Exception e) {
            // If not, refresh the connection.
            refresh();
        }
        return instance;
    }

    /**
     * Updates the current configuration for the IndyConnection if and only if the new configuration is different from the current one.
     * Closes (if it exists) the current connection and opens a new one with the new configuration.
     * @param configuration The new configuration to use.
     */
    public static void setConfiguration(IndyConfiguration configuration) {
        if(config == null || !config.equals(configuration)){
            config = configuration;
            refresh();
        }
    }

    public IndyPool getPool() {
        return poolInstance;
    }
    public IndyWallet getWallet() {
        return walletInstance;
    }
    public MessageEnvelopeCodec getCodec() {
        return codecInstance;
    }
    public IndyConfiguration getConfiguration() {
        return config;
    }

    @Override
    public void close() {
        try {
            walletInstance.close();
            poolInstance.close();
        } catch (Exception e) {
            Log.e("STUDYBITS", "Exception while closing indyConnection: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
