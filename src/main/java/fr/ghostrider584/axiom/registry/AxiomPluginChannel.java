package fr.ghostrider584.axiom.registry;

final class AxiomPluginChannel {
	static class Bidirectional {
		public static final String ANNOTATION_UPDATE = "axiom:annotation_update";
	}

	static class Server {
		public static final String ENABLE = "axiom:enable";
		public static final String RESTRICTIONS = "axiom:restrictions";
		public static final String MARKER_NBT_RESPONSE = "axiom:marker_nbt_response";
		public static final String MARKER_DATA = "axiom:marker_data";
		public static final String UPDATE_AVAILABLE_DISPATCH_SENDS = "axiom:update_available_dispatch_sends";
		public static final String RESPONSE_ENTITY_DATA = "axiom:response_entity_data";
	}

	static class Client {
		public static final String HELLO = "axiom:hello";
		public static final String SET_GAME_MODE = "axiom:set_gamemode";
		public static final String SET_FLY_SPEED = "axiom:set_fly_speed";
		public static final String SET_BUFFER = "axiom:set_buffer";
		public static final String SET_BLOCK = "axiom:set_block";
		public static final String TELEPORT = "axiom:teleport";
		public static final String MARKER_NBT_REQUEST = "axiom:marker_nbt_request";
		public static final String REQUEST_ENTITY_DATA = "axiom:request_entity_data";
		public static final String MANIPULATE_ENTITY = "axiom:manipulate_entity";
		public static final String DELETE_ENTITY = "axiom:delete_entity";
		public static final String SPAWN_ENTITY = "axiom:spawn_entity";
	}
}