# Jev Sales Conversation Copilot

A Zoom App that receives live Zoom Meeting transcripts through RTMS and uses TypeSafe AI's Jev model through OpenRouter to make bounded, confidence-scored sales decisions.

The app shows a turn-by-turn meeting transcript, buyer intent, objection, deal stage, purchase signal, deal risk, and up to three next-move choices ranked by Jev. The user identifies themselves as the seller or buyer, and the timeline labels each participant's side accordingly. The user can accept Jev's recommendation or select another reviewed coaching option. It does not automatically send a message or take an external action.

> Built with [RTMSManager](../../library/readme.md), the Zoom Apps SDK, and the OpenRouter Decisions API.

## Why Jev

Jev is a decision model, not a chat-completion model. It accepts state plus typed questions and returns choices, scores, and yes/no probabilities. It does not generate arbitrary response text.

This sample uses Jev for:

- `choice`: buyer intent, objection, deal stage, and next action.
- `noul`: probability that the buyer expressed a purchase signal.
- `score`: current deal risk on a defined five-level rubric.

The suggested phrase shown in the Zoom App is selected from reviewed application-owned text. This keeps the sample predictable and demonstrates Jev's intended role as a fast decision layer. Add a separate generative model only if your application genuinely needs custom prose.

## Architecture

```text
Zoom Meeting transcript
        |
        v
Zoom RTMS transcript socket
        |
        v
RTMSManager -> bounded per-stream conversation history
        |
        v
OpenRouter Decisions API -> TypeSafe AI Jev
        |
        v
typed sales decisions + probabilities
        |
        v
meeting-scoped WebSocket -> Zoom App sales cockpit
```

Conversation history and request ordering are isolated by `rtms_stream_id`. A stopped stream is removed from memory. The frontend only receives events for its registered meeting.

Each transcript turn is sent to the Zoom App immediately and shown while its Jev request is still running. The completed decision carries the same turn ID, so its choices are attached to the correct transcript entry even when several turns arrive close together. Very short turns remain visible but are not submitted to Jev.

## Buyer And Seller Roles

Use **My role** in the Zoom App to select `Seller` or `Buyer`. The choice is stored in that Zoom App browser session, so different app users can choose their own perspective without changing another user's view.

The app compares the current Zoom user context with each RTMS transcript speaker using participant IDs and then display name as a fallback:

- Your turns receive the selected role.
- Other participants receive the opposite role.
- Jev coaching choices are displayed on turns classified as buyer speech.
- Seller turns remain in the timeline and in Jev's bounded conversation context.

The backend still evaluates eligible turns before broadcasting the shared result because multiple Zoom App users in one meeting may choose different roles. For a production deployment that must reduce inference volume, maintain an explicit Zoom participant UUID to RTMS user ID mapping on the backend and evaluate only mapped buyer turns.

## Important RTMS Behavior

This sample does **not** call `zoomSdk.startRTMS()`. RTMS must already be started by the host, another Zoom App workflow, or an account-side process. Once Zoom delivers `meeting.rtms_started`, the backend connects and subscribes only to transcript media.

## Prerequisites

- Node.js 20 or newer.
- A Zoom Marketplace app with Zoom Apps and RTMS enabled.
- A public HTTPS URL for the Zoom App and webhook.
- An OpenRouter API key with access to `typesafe/jev-1.13`.
- Live meeting transcription available so RTMS transcript packets are produced.

## Configure the Zoom Marketplace App

1. Create or open a General App in the Zoom App Marketplace.
2. Enable the Zoom App surface and set the Home URL to your public HTTPS URL.
3. Add your application domain and `https://appssdk.zoom.us` to the Domain Allow List.
4. Enable these Zoom Apps SDK capabilities:
   - `getRunningContext`
   - `getMeetingUUID`
   - `getUserContext`
5. Enable RTMS for Zoom Meetings.
6. Subscribe the webhook endpoint to:
   - `meeting.rtms_started`
   - `meeting.rtms_stopped`
7. Set the webhook notification URL to `https://YOUR_DOMAIN_HERE/webhook`.
8. Copy the Client ID, Client Secret, and webhook Secret Token into `.env`.

The backend authenticates normal Zoom webhook deliveries with `x-zm-signature` and rejects stale timestamps. The endpoint-validation challenge is handled by `WebhookManager`.

## Environment

```bash
cp .env.example .env
```

Required values:

| Variable | Purpose |
|---|---|
| `ZOOM_CLIENT_ID` | Zoom Marketplace app client ID |
| `ZOOM_CLIENT_SECRET` | Signs the RTMS signaling and media handshakes |
| `ZOOM_SECRET_TOKEN` | Verifies Zoom webhook delivery signatures |
| `OPENROUTER_API_KEY` | Server-side key for OpenRouter |
| `PUBLIC_BASE_URL` | Public HTTPS origin used by the app |
| `FRONTEND_WSS_URL_TO_CONNECT_TO` | Public `wss://` URL ending in `/ws` |

Jev options:

| Variable | Default | Purpose |
|---|---:|---|
| `JEV_DECISIONS_URL` | `https://openrouter.ai/api/alpha/decisions` | OpenRouter's Decisions API endpoint |
| `JEV_MODEL` | `typesafe/jev-1.13` | TypeSafe AI Jev model ID |
| `JEV_TIMEOUT_MS` | `10000` | Maximum request duration |
| `JEV_HISTORY_TURNS` | `12` | Recent turns included in each decision request |
| `JEV_MIN_TRANSCRIPT_CHARACTERS` | `8` | Show, but do not analyze, very short transcript fragments |
| `SALES_CONTEXT` | `B2B software sales conversation` | Product and sales-motion context supplied to Jev |

Keep `.env` private. Only `.env.example` belongs in source control.

## Run

```bash
npm install
npm test
npm run check
npm start
```

Expose the configured port through an HTTPS reverse proxy or development tunnel, configure that URL in Marketplace, start RTMS externally, and open the app inside the active Zoom meeting.

## Decision Contract

The backend sends one request containing the recent transcript state and six parallel questions:

```json
{
  "model": "typesafe/jev-1.13",
  "state": {
    "sales_context": "B2B software sales conversation",
    "latest_turn": {
      "speaker": "Buyer",
      "text": "The price is higher than we budgeted."
    },
    "recent_turns": []
  },
  "questions": {
    "buyer_intent": { "type": "choice", "instructions": "...", "criteria": {} },
    "purchase_signal": { "type": "noul", "instructions": "..." },
    "deal_risk": { "type": "score", "instructions": "...", "criteria": [] }
  }
}
```

Question definitions and application-owned coaching phrases are in [`jevSalesAnalyzer.js`](jevSalesAnalyzer.js).

## Current Assumptions

- Role assignment is viewer-specific and uses ID/name matching. For production, maintain an explicit Zoom participant UUID to RTMS user ID mapping when duplicate display names or renamed participants are possible.
- Jev decisions are guidance, not ground truth. Display confidence, test on representative calls, and keep consequential actions under human control.
- Transcript text is sent to OpenRouter and its underlying provider. Review provider data-handling terms before using confidential meetings.
- Backend context is bounded and in memory. The Zoom App displays up to 100 turns for the current browser session; it does not persist or recover transcript history after a reload.

## Project Files

| File | Purpose |
|---|---|
| `index.js` | RTMS, authenticated webhook, meeting-scoped frontend WebSocket, and lifecycle handling |
| `jevSalesAnalyzer.js` | Jev question contract, Decisions API client, per-stream history, and coaching policy |
| `public/index.ejs` | Zoom App sales-copilot UI |
| `tests/jevSalesAnalyzer.test.js` | Offline decision-contract and response-normalization tests |
| `.env.example` | Safe configuration template |
| `Dockerfile` | Platform-agnostic multi-stage container build |

## Docker

Build from the repository root because the image includes the shared JavaScript library:

```bash
docker build \
  -f zoom_apps/jev_transcript_analysis_js/Dockerfile \
  -t rtms-jev-sales-copilot .

docker run --rm \
  --env-file zoom_apps/jev_transcript_analysis_js/.env \
  -p 3000:3000 \
  rtms-jev-sales-copilot
```

## Troubleshooting

| Symptom | Check |
|---|---|
| Zoom App says registration failed | RTMS must already be active for the same meeting before frontend registration |
| No transcript appears | Confirm live transcription and RTMS transcript media are enabled |
| Decisions API returns 400 | Use the Decisions endpoint, not `/chat/completions`, and verify `JEV_MODEL` |
| Decisions API returns 401 | Verify `OPENROUTER_API_KEY` in the server-side `.env` |
| Zoom App SDK is blocked | Add your domain and `https://appssdk.zoom.us` to the Marketplace allow list |
