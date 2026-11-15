# clinic-backend

## Staff authentication and sessions

Staff can sign in with `POST /api/auth/login` using an email address and password:

```json
{
  "email": "doctor@example.com",
  // doctor@demo-dental.local
  "password": "your-password"
  // 1234
}
```

The response contains the same Bearer JWT in both `accessToken` and `token` fields for dashboard compatibility. Send it as `Authorization: Bearer <token>` when calling `/api/staff/**`. Tokens include the staff ID, clinic ID, and role and expire after `JWT_EXPIRATION_SECONDS` (one hour by default). Staff booking and session queries use the clinic ID in the verified token.

All staff passwords must be stored as BCrypt hashes. The development seed account is configured by `DEMO_DOCTOR_EMAIL` and `DEMO_DOCTOR_PASSWORD`.

Create a session with `POST /api/staff/sessions` using `type`, `date`, `startTime`, `endTime`, `maxAppointments`, `avgMinutes`, and ISO-8601 `bookingOpensAt`. The authenticated active doctor is assigned to the session. Update those scheduling fields (excluding `type` and `date`) with `PATCH /api/staff/sessions`, including the session `id`. Change booking status with `PATCH /api/staff/sessions/{id}/status` and an `action` of `Open`, `Pause`, or `Close`.

Get a session's ordered bookings with `GET /api/staff/sessions/{id}/queue`. Advance the queue with `POST /api/staff/queue/call-next` and a JSON `sessionId`; the current serving booking is completed and the next booked booking starts. When there is no next booking, the response sets `queueEmpty` to `true`. Call a specific booked token with `POST /api/staff/queue/call/{bookingId}`; it moves to `CALLED`.

Set `JWT_SECRET` to a private Base64-encoded key of at least 256 bits outside local development. The built-in key is for development only and must not be used in production. `/api/public/**` remains accessible without authentication.
