package ransomaware;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.InvalidUserNameException;
import ransomaware.exceptions.NoSuchUserException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private static final ConcurrentHashMap<Integer, SessionObject> sessions = new ConcurrentHashMap<>();
    
    public enum SessionState {
        VALID,
        INVALID,
        EXPIRED
    }

    public static SessionState getSessionSate(int sessionToken) {
        if (sessions.containsKey(sessionToken)) {
            if (sessions.get(sessionToken).expirationMoment.isAfter(Instant.now())) {
                return SessionState.VALID;
            } else {
                sessions.remove(sessionToken);
                return SessionState.EXPIRED;
            }
        }
        return SessionState.INVALID;
    }

    public static String getUsername(Integer sessionToken) {
        if (sessions.containsKey(sessionToken)) {
            return sessions.get(sessionToken).username;
        }
        // TODO: else should never happen but should still be checked
        return null;
    }

    public static String getEncryptCertificate(String username) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("_id", username);
        DBObject userQuery = client.getDB(ServerVariables.FS_PATH)
                .getCollection(ServerVariables.DB_COLLECTION_USERS)
                .findOne(query);
        client.close();

        if (userQuery != null) {
            return (String) userQuery.get("encryptCert");
        }
        throw new NoSuchUserException();
    }

    public static String getSigningCertificate(String username) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("_id", username);
        DBObject userQuery = client.getDB(ServerVariables.FS_PATH)
                .getCollection(ServerVariables.DB_COLLECTION_USERS)
                .findOne(query);
        client.close();

        if (userQuery != null) {
            return (String) userQuery.get("signCert");
        }
        throw new NoSuchUserException();
    }

    public static void hasUser(String username) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("_id", username);
        DBObject userQuery = client.getDB(ServerVariables.FS_PATH)
                .getCollection(ServerVariables.DB_COLLECTION_USERS)
                .findOne(query);
        client.close();

        if (userQuery != null) {
            return;
        }
        throw new NoSuchUserException();
    }

    public static void register(String username, String password, String encryptCert, String signCert) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("_id", username);
        var users = client.getDB(ServerVariables.FS_PATH)
                .getCollection(ServerVariables.DB_COLLECTION_USERS);
        var salts = client.getDB(ServerVariables.FS_PATH)
                .getCollection(ServerVariables.DB_COLLECTION_SALTS);
        var user = users.findOne(query);

        // Check if username is illegal, like "daniel/joao" or ".."
        if (username.matches("[.]*") || username.contains("/")) {
            throw new InvalidUserNameException();
        }

        if (user == null) {
            SecureRandom rand = new SecureRandom();
            byte[] salt = new byte[64];
            rand.nextBytes(salt);

            String passwordDigest = SecurityUtils
                    .getBase64(
                            SecurityUtils.getDigest(password + new String(salt) + ServerVariables.PASSWORD_SALT)
                    );
            var obj = new BasicDBObject("_id", username)
                    .append("password", passwordDigest)
                    .append("encryptCert", encryptCert)
                    .append("signCert", signCert);
            users.insert(obj);
            obj = new BasicDBObject("_id", username)
                    .append("salt", SecurityUtils.getBase64(salt));
            salts.insert(obj);
            client.close();
        } else {
            client.close();
            throw new DuplicateUsernameException();
        }

    }

    public static int login(String username, String password) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("_id", username);
        DBObject userQuery = client.getDB(ServerVariables.FS_PATH)
                .getCollection(ServerVariables.DB_COLLECTION_USERS)
                .findOne(query);
        DBObject saltQuery = client.getDB(ServerVariables.FS_PATH)
                .getCollection(ServerVariables.DB_COLLECTION_SALTS)
                .findOne(query);
        client.close();

        if (userQuery != null) {
            byte[] salt = SecurityUtils.decodeBase64((String)saltQuery.get("salt"));
            String digest = SecurityUtils
                    .getBase64(
                            SecurityUtils.getDigest(password + new String(salt) + ServerVariables.PASSWORD_SALT)
                    );
            if (userQuery.get("password").equals(digest)) {
                SecureRandom rand = new SecureRandom();
                int token = rand.nextInt();
                sessions.put(token, new SessionObject(username, Instant.now().plusSeconds(ServerVariables.SESSION_DURATION)));
                return token;
            }
        }
        throw new UnauthorizedException();
    }

    public static void logout(int sessionToken) {
        try {
            sessions.remove(sessionToken);
        } catch (NullPointerException ignored) {
            //ignored
        }
    }

    private static MongoClient getMongoClient() {
        return new MongoClient(new MongoClientURI(ServerVariables.MONGO_URI));
    }

    private static class SessionObject {
        private final String username;
        private final Instant expirationMoment;

        public SessionObject(String username, Instant expirationMoment) {
            this.username = username;
            this.expirationMoment = expirationMoment;
        }
    }

    public static String createSessionCookie(int token) {
        StringBuilder c = new StringBuilder()
                .append("login-token=")
                .append(token)
                .append("; HttpOnly; Secure; Version=1; max-age=")
                .append(ServerVariables.SESSION_DURATION);
        return c.toString();
    }
}
