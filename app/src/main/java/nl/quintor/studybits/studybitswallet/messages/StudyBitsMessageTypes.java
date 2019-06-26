package nl.quintor.studybits.studybitswallet.messages;

import nl.quintor.studybits.indy.wrapper.dto.CredentialOfferList;
import nl.quintor.studybits.indy.wrapper.message.IndyMessageTypes;
import nl.quintor.studybits.indy.wrapper.message.MessageType;
import nl.quintor.studybits.indy.wrapper.message.MessageTypes;

import java.util.concurrent.atomic.AtomicBoolean;


public class StudyBitsMessageTypes {
    private static String STUDYBITS_URN_PREFIX = "urn:studybits:sov:agent:message_type:quintor.nl/";
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static MessageType<AuthcryptableExchangePositions> EXCHANGE_POSITIONS = new IndyMessageTypes.StandardMessageType<>(
            STUDYBITS_URN_PREFIX + "exchange_position/1.0/exchangePositions", MessageType.Encryption.AUTHCRYPTED, AuthcryptableExchangePositions.class);

    public static MessageType<CredentialOfferList> DOCUMENT_OFFERS = new IndyMessageTypes.StandardMessageType<>(
            STUDYBITS_URN_PREFIX + "document/1.0/documentOffers", MessageType.Encryption.AUTHCRYPTED, CredentialOfferList.class);

    public static void init() {
//        log.debug("Trying to initialize message types");
        if (!initialized.get()) {
            if(initialized.compareAndSet(false, true)) {
//                log.debug("Initializing message types");
                MessageTypes.registerType(EXCHANGE_POSITIONS);
                MessageTypes.registerType(DOCUMENT_OFFERS);
            }
        }
    }
}
