export const TYPE_FLAGS = {
  audio: 1,
  video: 2,
  sharescreen: 4,
  transcript: 8,
  chat: 16
};

export class RTMSFlagHelper {
  /**
   * Calculate effective media flags based on requested flags and available server URLs
   * @param {number} requestedFlags - Combined concrete media type flags
   * @param {Object} serverUrls - The server_urls object from the handshake response
   * @returns {number} The effective flags to use for connection
   */
  static calculateEffectiveFlags(requestedFlags, serverUrls = {}) {
    let effectiveFlags = 0;
    for (const [type, flag] of Object.entries(TYPE_FLAGS)) {
      if ((requestedFlags & flag) && serverUrls[type]) {
        effectiveFlags |= flag;
      }
    }
    
    return effectiveFlags;
  }
}
