/*
 *    Copyright 2016 5zig
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.the5zig.mod.server;

import eu.beezig.core.server.ServerHive;
import eu.the5zig.mod.The5zigAPI;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class that holds all game mode listeners and contains util methods to send & ignore server messages.
 */
public class GameListenerRegistry {

	private GameMode current;
	public static final List<AbstractGameListener<? extends GameMode>> gameListeners = new ArrayList<>();
	private static final HashMap<String, Pattern> messages = new HashMap<>();


	/**
	 * Registers a listener that listens on a game mode.
	 *
	 * @param listener the listener that should be registered.
	 */
	public void registerListener(AbstractGameListener<? extends GameMode> listener) {
		listener.gameListener = this;
		gameListeners.add(listener);
	}

	/**
	 * Switches the lobby of a server and tries to match a new game mode by iterating through all listeners
	 * and calling {@link AbstractGameListener#matchLobby(String)}.
	 *
	 * @param newLobby the new lobby.
	 */
	public void switchLobby(String newLobby) {

		boolean gameModeFound = false;
		if (newLobby != null) {
			for (final AbstractGameListener gameListener : gameListeners) {
				if (gameListener.matchLobby(newLobby)) {
					try {
						current = (GameMode) gameListener.getGameMode().newInstance();
						gameListener.onGameModeJoin(current);
						System.out.println(current.getClass());

					}
					catch (Throwable throwable) {
						throwable.printStackTrace();
					}
					gameModeFound = true;
					break;
				}
			}
		}
		if (!gameModeFound) {
			current = null;
		}
	}

	/**
	 * @return the current game mode or {@code null}, if the client does not play on this server instance.
	 */
	public GameMode getCurrentGameMode() {
		return The5zigAPI.getAPI().getActiveServer() instanceof ServerHive ? current : null;
	}

	public static void loadPatterns() {
		final String path = "/core/messages/hive.properties";
		if (!messages.isEmpty()) {
			return;
		}
		final Properties properties = new Properties();
		try {
			properties.load(GameListenerRegistry.class.getResourceAsStream(path));
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		final Set<Map.Entry<Object, Object>> enumeration = properties.entrySet();
		for (final Map.Entry<Object, Object> entry : enumeration) {
			try {
				String regex = String.valueOf(entry.getValue());
				regex = regex.replace("%p", "\\w{1,16}");
				regex = regex.replace("%d", "-?[0-9]+");
				messages.put(String.valueOf(entry.getKey()), Pattern.compile(regex));
			}
			catch (PatternSyntaxException ignored) {
			}
		}
	}

	public boolean match(final String message) {
		boolean ignore = false;
		for (final String key : messages.keySet()) {
			final MatchResult match = this.match(message, key);
			if(match.ignore) ignore = true;
		}
		return ignore;
	}

	public String getCurrentLobby() {
		return "TODO CHANGE"; //TODO Change
	}

	public MatchResult match(final String message, final String key) {
		if (!messages.containsKey(key)) {
			return null;
		}
		final Pattern pattern = messages.get(key);
		final Matcher matcher = pattern.matcher(message);
		if (matcher.matches()) {
			final List<String> matches = new ArrayList<>();
			for (int i = 1; i <= matcher.groupCount(); ++i) {
				matches.add(matcher.group(i));
			}
			IPatternResult res = new IPatternResult(matches);
			for(AbstractGameListener list : gameListeners) {
				if(list.getGameMode() != null && current != null && !list.getGameMode().isAssignableFrom(current.getClass()))
					continue;
				list.onMatch(current, key, res);
			}
			MatchResult result = new MatchResult();
			result.ignore = res.shouldIgnore();
			result.matches = matches;
			return result;
		}
		return new MatchResult();
	}

	private static class MatchResult {
		boolean ignore;
		List<String> matches;
	}
}
