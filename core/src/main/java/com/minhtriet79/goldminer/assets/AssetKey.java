package com.minhtriet79.goldminer.assets;

/**
 * Semantic asset keys decoupled from concrete theme file paths so themes
 * can be swapped without modifying gameplay or screen code.
 */
public final class AssetKey {

    // Backgrounds & Tracks (World 1 Australia)
    public static final String BG_MENU_16BIT = "bg_menu_16bit";
    public static final String BG_HUB_CANYON = "bg_hub_canyon";
    public static final String BG_MAP_AUSTRALIA = "bg_map_australia";
    public static final String BG_SKY_W1 = "bg_sky_w1";
    public static final String BG_CAVE_W1 = "bg_cave_w1";
    public static final String BG_MINE_W1 = "bg_mine_w1";
    public static final String BG_PASS_W1 = "bg_pass_w1";
    public static final String BG_RESULT_W1 = "bg_result_w1";
    public static final String BG_SHOP_INTERIOR = "bg_shop_interior";
    public static final String BG_SHOP_FRAME = "bg_shop_frame";
    public static final String TRACK_W1 = "track_w1";

    public static String trackKey(int trackN) {
        int clamped = Math.max(1, Math.min(9, trackN));
        return "track_" + clamped;
    }

    // Cart & 4 Claws (Swing/Closed, Extend/Open, Retract/Closed)
    public static final String CART_STANDARD = "cart_standard";
    public static final String HOOK_CLAW = "hook_claw";

    public static String clawKey(int clawId, String stateSuffix) {
        int clamped = Math.max(1, Math.min(4, clawId));
        return "claw_" + clamped + "_" + stateSuffix;
    }

    // Mine Items (World 1)
    public static final String GOLD_SMALL = "gold_small";
    public static final String GOLD_SMALL_MED = "gold_small_med";
    public static final String GOLD_MEDIUM = "gold_medium";
    public static final String GOLD_LARGE = "gold_large";
    public static final String GOLD_MEGA = "gold_mega";
    public static final String GOLD_PAN = "gold_pan";
    public static final String ROCK_SMALL = "rock_small";
    public static final String ROCK_MEDIUM = "rock_medium";
    public static final String ROCK_LARGE = "rock_large";
    public static final String DIAMOND = "diamond";
    public static final String MYSTERY_BAG = "mystery_bag";
    public static final String TNT_BARREL = "tnt_barrel";
    public static final String SKULL = "skull";
    public static final String BONE_SMALL = "bone_small";
    public static final String BONE_LONG_V = "bone_long_v";
    public static final String BONE_LONG_H = "bone_long_h";
    public static final String SKELETON = "skeleton";
    public static final String GEM_1 = "gem_1";
    public static final String GEM_2 = "gem_2";
    public static final String GEM_3 = "gem_3";
    public static final String GEM_4 = "gem_4";

    // UI Buttons & Chrome
    public static final String BTN_MENU_UP = "btn_menu_up";
    public static final String BTN_MENU_OVER = "btn_menu_over";
    public static final String BTN_MENU_DOWN = "btn_menu_down";
    public static final String BTN_PAUSE_UP = "btn_pause_up";
    public static final String BTN_PAUSE_OVER = "btn_pause_over";
    public static final String BTN_PAUSE_DOWN = "btn_pause_down";
    public static final String BTN_SOUND_ON_UP = "btn_sound_on_up";
    public static final String BTN_SOUND_ON_OVER = "btn_sound_on_over";
    public static final String BTN_SOUND_ON_DOWN = "btn_sound_on_down";
    public static final String BTN_SOUND_OFF = "btn_sound_off";
    public static final String BTN_NEXT_UP = "btn_next_up";
    public static final String BTN_NEXT_OVER = "btn_next_over";
    public static final String BTN_NEXT_DOWN = "btn_next_down";
    public static final String BTN_HELP_UP = "btn_help_up";
    public static final String BTN_HELP_OVER = "btn_help_over";
    public static final String BTN_HELP_DOWN = "btn_help_down";
    public static final String BTN_WINDOW_UP = "btn_window_up";
    public static final String BTN_WINDOW_OVER = "btn_window_over";
    public static final String BTN_WINDOW_DOWN = "btn_window_down";
    public static final String UI_VEGAS_FRAME = "ui_vegas_frame";
    public static final String UI_VEGAS_LOGO = "ui_vegas_logo";

    public static final String UI_DYNAMITE_ICON = "ui_dynamite_icon";
    public static final String SHOP_DESC_BANNER = "shop_desc_banner";
    public static final String SHOP_SPEECH_BUBBLE = "shop_speech_bubble";

    // Main Menu 16-bit & Vegas Marquee / Play UI
    public static final String MENU_AVATAR = "menu_avatar";
    public static final String MENU_COIN_ICON = "menu_coin_icon";
    public static final String MENU_BULB_LIT = "menu_bulb_lit";
    public static final String MENU_BULB_GLOW = "menu_bulb_glow";
    public static final String BTN_PLAY_UP = "btn_play_up";
    public static final String BTN_PLAY_OVER = "btn_play_over";
    public static final String BTN_PLAY_GLOW = "btn_play_glow";

    public static final String UI_PANEL_AUTH = "ui_panel_auth";
    public static final String UI_ACCOUNT_CARD = "ui_account_card";
    public static final String UI_ACCOUNT_DROPDOWN = "ui_account_dropdown";
    public static final String UI_MODAL_PANEL = "ui_modal_panel";
    public static final String UI_RENAME_PANEL = "ui_rename_panel";
    public static final String UI_BTN_TICKET_BLUE_UP = "ui_btn_ticket_blue_up";
    public static final String UI_BTN_TICKET_BLUE_OVER = "ui_btn_ticket_blue_over";
    public static final String UI_BTN_TICKET_RED_UP = "ui_btn_ticket_red_up";
    public static final String UI_BTN_TICKET_RED_OVER = "ui_btn_ticket_red_over";
    public static final String UI_TAB_ACTIVE = "ui_tab_active";
    public static final String UI_TAB_INACTIVE = "ui_tab_inactive";
    public static final String UI_INPUT_NORMAL = "ui_input_normal";
    public static final String UI_INPUT_FOCUS = "ui_input_focus";
    public static final String UI_CHECKBOX_OFF = "ui_checkbox_off";
    public static final String UI_CHECKBOX_ON = "ui_checkbox_on";
    public static final String UI_BTN_GREEN_UP = "ui_btn_green_up";
    public static final String UI_BTN_GREEN_OVER = "ui_btn_green_over";
    public static final String UI_BTN_BLUE_UP = "ui_btn_blue_up";
    public static final String UI_BTN_BLUE_OVER = "ui_btn_blue_over";
    public static final String UI_BTN_RED_UP = "ui_btn_red_up";
    public static final String UI_BTN_RED_OVER = "ui_btn_red_over";

    // World Hub, Gold Mine Selector, & Australia Map UI
    public static final String HUB_CARD_MINE = "hub_card_mine";
    public static final String HUB_CARD_CASINO = "hub_card_casino";
    public static final String HUB_CARD_ARCADE = "hub_card_arcade";
    public static final String HUB_CARD_SHOP = "hub_card_shop";
    public static final String MODE_CARD_1P = "mode_card_1p";
    public static final String MODE_CARD_2P = "mode_card_2p";
    public static final String UI_HUB_CARD_FRAME = "ui_hub_card_frame";
    public static final String UI_HUB_CARD_FRAME_OVER = "ui_hub_card_frame_over";
    public static final String UI_MODE_CARD_FRAME = "ui_mode_card_frame";
    public static final String UI_MODE_CARD_FRAME_OVER = "ui_mode_card_frame_over";
    public static final String UI_BTN_BACK_UP = "ui_btn_back_up";
    public static final String UI_BTN_BACK_OVER = "ui_btn_back_over";
    public static final String UI_TITLE_BANNER = "ui_title_banner";
    public static final String UI_TITLE_BANNER_WIDE = "ui_title_banner_wide";
    public static final String UI_MAP_PREVIEW = "ui_map_preview";
    public static final String MAP_MINI_MINER_IDLE = "map_mini_miner_idle";
    public static final String MAP_MINI_MINER_WALK = "map_mini_miner_walk";
    public static final String MAP_GOLD_CAVE = "map_gold_cave";
    public static final String MINER_MINI = "miner_mini";
    public static final String MAP_NODE_CLEARED = "map_node_cleared";
    public static final String MAP_NODE_UNLOCKED = "map_node_unlocked";
    public static final String MAP_NODE_SELECTED = "map_node_selected";
    public static final String MAP_NODE_LOCKED = "map_node_locked";
    public static final String MAP_NODE_GLOW = "map_node_glow";
    public static final String MAP_CHECK_BADGE = "map_check_badge";
    public static final String MAP_LOCK_SMALL = "map_lock_small";
    public static final String MAP_LOCK_BIG = "map_lock_big";

    // Animations - Miner, FX, Creatures, Shopkeeper
    public static final String MINER_IDLE = "miner_idle";
    public static final String MINER_DOWN = "miner_down";
    public static final String MINER_UP = "miner_up";
    public static final String MINER_THROW = "miner_throw";
    public static final String MINER_BRAKE = "miner_brake";
    public static final String MINER_STRENGTH = "miner_strength";

    public static final String FX_EXPLOSION = "fx_explosion";
    public static final String FX_SMOKE = "fx_smoke";
    public static final String FX_DYNAMITE_FLY = "fx_dynamite_fly";

    public static final String BILBY_WALK = "bilby_walk";
    public static final String BILBY_DIAMOND_WALK = "bilby_diamond_walk";
    public static final String TASMANIAN_WALK = "tasmanian_walk";

    public static final String LEX_TALK = "lex_talk";
    public static final String LEX_BUY_ARM = "lex_buy_arm";

    // Audio - Music
    public static final String BGM_MENU = "bgm_menu";
    public static final String BGM_W1 = "bgm_w1";

    // Audio - SFX
    public static final String SFX_HOOK_EXTEND = "sfx_hook_extend";
    public static final String SFX_HOOK_RETRACT = "sfx_hook_retract";
    public static final String SFX_COIN = "sfx_coin";
    public static final String SFX_TALLY = "sfx_tally";
    public static final String SFX_EXPLOSION = "sfx_explosion";
    public static final String SFX_COUNTDOWN_BEEP = "sfx_countdown_beep";
    public static final String SFX_PASS = "sfx_pass";
    public static final String SFX_NOPASS = "sfx_nopass";
    public static final String SFX_HEAVY_GRAB = "sfx_heavy_grab";
    public static final String SFX_CABLE_BRAKE = "sfx_cable_brake";
    public static final String SFX_SHOP_OPEN = "sfx_shop_open";
    public static final String SFX_SHOP_BUY = "sfx_shop_buy";
    public static final String SFX_SHOP_TALK = "sfx_shop_talk";
    public static final String SFX_CART_1 = "sfx_cart_1";
    public static final String SFX_CART_2 = "sfx_cart_2";
    public static final String SFX_CART_3 = "sfx_cart_3";
    public static final String SFX_GEM_HIT = "sfx_gem_hit";
    public static final String SFX_GEM_COMBO = "sfx_gem_combo";

    private AssetKey() {
    }
}
