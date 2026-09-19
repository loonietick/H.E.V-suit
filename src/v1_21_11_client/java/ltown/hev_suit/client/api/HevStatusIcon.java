package ltown.hev_suit.client.api;

/**
 * The HUD status pips a companion mod can drive through {@link HevSuitApi#setStatusIcon}.
 * NERVEGAS and WASTE have shipped textures but no wired-up logic anywhere in the base mod today --
 * this is the first real code path for either of them.
 */
public enum HevStatusIcon {
    COLD,
    ELECTRICAL,
    RADIATION,
    BIOHAZARD,
    NERVEGAS,
    WASTE
}
