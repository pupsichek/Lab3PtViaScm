package com.example.pt.lab3.configuration.ws;

import com.example.pt.lab3.util.UUIDUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode
public class Destination {
    @Getter
    private final String endpoint;
    @Getter
    private final TypeDestination type;

    private static Destination SUBSCRIBE_GAMES_ENDPOINT = new Destination(TypeDestination.SUBSCRIBE, "/user/queue/games");
    private static Destination SEND_GAMES_ENDPOINT = new Destination(TypeDestination.SEND, "/queue/games");


    private Destination(TypeDestination type, String endpoint, Object... params) {
        this.type = type;
        this.endpoint = String.format(endpoint, params);
    }

    /**
     * Get destination all games during subscribe
     */
    public static String getSubscribeGamesEndpoint() {
        return SUBSCRIBE_GAMES_ENDPOINT.getEndpoint();
    }

    /**
     * Get destination all games during send
     */
    public static String getSendGamesEndpoint() {
        return SEND_GAMES_ENDPOINT.getEndpoint();
    }

    /**
     * Get destination for specified game during subscribe
     */
    public static String getSubscribeGameEndpoint(UUID gameId) {
        return new Destination(TypeDestination.SUBSCRIBE, "/user/queue/game?id=%s", gameId).getEndpoint();
    }

    /**
     * Get destination for specified game during send
     */
    public static String getSendGameEndpoint(UUID gameId) {
        return new Destination(TypeDestination.SEND, "/queue/game?id=%s", gameId).getEndpoint();
    }

    public enum TypeDestination {
        SUBSCRIBE, SEND
    }

    /**
     * Utils for destinations
     * Can check what is the destination?
     * Can convert subscribe destination to send type.
     * Can get game id from specified game destination.
     */
    public static class Utils {
        private static final Predicate<String> patternEndedGame = Pattern.compile("/user/queue/game\\?id=[a-zA-Z0-9\\-]+$").asPredicate();
        private static final Predicate<String> patternEndedGames = Pattern.compile("/user/queue/games$").asPredicate();
        private static final Pattern getGameIdPattern = Pattern.compile("[a-zA-Z0-9\\-]+$");

        /**
         * Check that destination is specified game destination
\         */
        public static boolean isSpecifyGameDestination(String endpoint) {
            return patternEndedGame.test(endpoint);
        }

        /**
         * Check that destination is all games destination
         */
        public static boolean isGamesDestination(String endpoint) {
            return patternEndedGames.test(endpoint);
        }

        /**
         * Convert subscribe destination to send destination
         */
        public static String convertSubscribeDestToSendDest(String endpoint) {
            return StringUtils.startsWith(endpoint, "/user") ? endpoint.substring(5) : endpoint;
        }

        /**
         * Get id as UUID from specified game destination
         * @return Optional of id, if id cannot convert or empty => return null
         */
        public static Optional<UUID> getGameIdFromSpecifiedGameEndpoint(String endpoint) {
            Optional<String> strId = getIdAsStrFromSpecifiedGameEndpoint(endpoint);
            if (strId.isPresent()) {
                if (UUIDUtils.isValidUUID(strId.get())) {
                    return Optional.of(UUID.fromString(strId.get()));
                }
                return Optional.empty();
            } else {
                return Optional.empty();
            }
        }

        /**
         * Get id as string from specified game destination
         */
        public static Optional<String> getIdAsStrFromSpecifiedGameEndpoint(String endpoint) {
            Matcher matcher = getGameIdPattern.matcher(endpoint);
            if (matcher.find()) {
                return Optional.of(matcher.group());
            }
            return Optional.empty();
        }
    }
}
