# API assumptions to verify against Spring Boot

The architecture document defines the request fields, but not the exact successful response bodies. The Android project therefore treats every HTTP 2xx response as success and does not deserialize claim/upload responses.

## Currently implemented

### Claim body field names

- `deviceId`
- `pairingCode`
- `claimToken`

### Upload multipart field names

- `image`
- `deviceId`
- `claimToken`

### Paths

- `/api/v1/ocr/scans/{scanId}/claim`
- `/api/v1/ocr/scans/{scanId}/image`

## Verify later

1. Maximum accepted image size.
2. Accepted MIME types.
3. Whether the upload part is definitely named `image` rather than `file`.
4. Whether claim/upload success uses 200, 201, 202, or 204.
5. Whether the backend requires an authorization header in addition to the claim token.
6. Whether the claim token has a shorter expiry than the desktop pairing dialog indicates.

Only the two networking files need adjustment if these details change.
