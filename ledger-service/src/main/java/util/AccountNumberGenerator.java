package util;

import java.security.SecureRandom;

public class AccountNumberGenerator {

    private static final SecureRandom random = new SecureRandom();

    private AccountNumberGenerator() { }

    public static String generate() {
        long timestamp = System.currentTimeMillis();
        int randomPart = random.nextInt(999);
        return "ACC%013d%04d".formatted(timestamp, randomPart);
    }
}
