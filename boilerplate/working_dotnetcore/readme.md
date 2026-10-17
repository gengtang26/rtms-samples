# Zoom RTMS Media Receiver (.NET Core)

This .NET Core example demonstrates split RTMS media connections selected with readable media names. The example configuration selects audio, active-speaker video, and transcript data.

The server connects to Zoom’s RTMS infrastructure via WebSocket, handles webhook events, and logs media events via the console.

## Prerequisites

- .NET 6.0 SDK or later
- A Zoom account with RTMS enabled
- Zoom App credentials (Client ID and Client Secret)
- Zoom Secret Token for webhook validation

## Setup

1. Restore dependencies:
```bash
dotnet restore
```

2. Create a `.env` file in the root directory with the following content:
```
ZOOM_SECRET_TOKEN=your_secret_token
ZOOM_CLIENT_ID=your_client_id
ZOOM_CLIENT_SECRET=your_client_secret
PORT=3000
WEBHOOK_PATH=/webhook
RTMS_MEDIA_TYPES=audio,video,transcript
```

## Running the Example

1. Start the server:
```bash
dotnet run
```

2. Expose your server using a tool like ngrok:
```bash
ngrok http 3000
```

3. Configure your Zoom App's webhook endpoint to match the ngrok URL:
```
https://<your-ngrok-subdomain>.ngrok.io/webhook
```

4. Start a Zoom meeting and enable RTMS.

## How it Works

1. The server listens for RTMS webhook events at the configured path.
2. On receiving a `meeting.rtms_started` event, it connects to Zoom’s signaling WebSocket server.
3. Upon successful signaling handshake, it opens one data socket for each name in `RTMS_MEDIA_TYPES`.
4. With `audio,video,transcript`, each media socket sends its own handshake using `media_type` values `1`, `2`, and `8` with only the matching media parameters.
5. Each successful media handshake sends a `CLIENT_READY_ACK` through the signaling socket.
6. Audio, video, and transcript messages use `msg_type` values `14`, `15`, and `17`.
7. Messages are printed to the console to confirm receipt.

`RTMS_MEDIA_TYPES` accepts `audio`, `video`, `screenshare`, `transcript`, and
`chat`. Each selected name opens a separate data connection.

```env
RTMS_MEDIA_TYPES=audio,video,transcript
```

> Unified media socket mode and `media_type: 32` are deprecated. RTMS clients
> must open a separate data connection for each required media type and send one
> concrete media type per handshake: audio (`1`), video (`2`), screen share
> (`4`), transcript (`8`), or chat (`16`).

## Notes

- This example uses `System.Net.WebSockets` for real-time streaming and `System.Text.Json` for JSON processing.
- Keep-alive messages are handled automatically for both signaling and media connections.
- The app is implemented using the ASP.NET Core Minimal API model.
- No frontend interface (HTML) is provided in this example.
- This is a logging-only example; modify the handlers to save or process the media content as needed.

## Security

- Never commit your `.env` file or secrets to version control.
- Use HTTPS in production and validate incoming webhook requests for authenticity.

## Docker

The project runs the ASP.NET Core RTMS webhook and media receiver. Its multi-stage Dockerfile keeps build tooling out of the final runtime image and does not hard-code a CPU architecture.

Build and run it from the `rtms-samples` repository root:

```bash
docker build -f boilerplate/working_dotnetcore/Dockerfile -t rtms-boilerplate-working_dotnetcore .
docker run --rm --env-file boilerplate/working_dotnetcore/.env -p 3000:3000 rtms-boilerplate-working_dotnetcore
```

Run the build from the repository root because the Dockerfile uses repository-relative paths. Runtime secrets are supplied with `--env-file` and are excluded from the image build context.

## Webhook Delivery Authentication

Normal Zoom webhook deliveries are verified against the exact raw request body using
`x-zm-signature` and `x-zm-request-timestamp`. Configure `ZOOM_SECRET_TOKEN` with the
Marketplace app's webhook Secret Token. Requests with missing, invalid, or stale
signatures are rejected; the default replay window is 300 seconds and can be changed
with `WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS`.
