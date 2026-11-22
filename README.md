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

Create an online booking with `POST /api/public/bookings` using `sessionId`, `patientName`, `phone`, and optional `address`. Booking details returned in the staff queue include the patient's `address`.

Get a session's ordered bookings with `GET /api/staff/sessions/{id}/queue`. Advance the queue with `POST /api/staff/queue/call-next` and a JSON `sessionId`; the current serving booking is completed and the next booked booking starts. When there is no next booking, the response sets `queueEmpty` to `true`. Call a specific booked or called token with `POST /api/staff/queue/call/{bookingId}`; it moves to `SERVING` and any current serving token becomes `DONE`.

For today's session by clinic slug and session type, call the next patient with `POST /api/clinics/{slug}/sessions/{type}/call-next` (for example, `/api/clinics/demo-dental/sessions/EVENING/call-next`). This secured endpoint requires a staff Bearer token and returns the updated ordered `queue` along with the completed and serving bookings.

Alternatively, call a selected patient directly with `PATCH /api/clinics/{slug}/bookings/{bookingId}/status`. The selected booked or called patient becomes `SERVING`, and any currently serving patient in that session becomes `DONE`.

Skip a booked, called, or serving token with `POST /api/staff/queue/{bookingId}/skip`; it increments `skipCount` and advances to the next booked token when no other token is currently serving. Requeue a skipped or no-show token with `POST /api/staff/queue/{bookingId}/requeue`; it returns to `BOOKED` without changing `tokenNo` and is placed immediately after the serving token (or at the end if no token is serving). Mark an unserved token as a no-show with `POST /api/staff/queue/{bookingId}/no-show`. Add a walk-in with `POST /api/staff/bookings/walk-in` and `sessionId`, `patientName`, `phone`, and optional `address`; walk-ins are appended to that session's queue.

Staff queue changes are recorded in the clinic-scoped audit log with staff ID, token number, action, and before/after status. Fetch it with `GET /api/staff/audit-log`. Undo the most recent audited action for a booking with `POST /api/staff/queue/undo` and JSON `{"bookingId":"<booking UUID>"}`. Undo is rejected if the booking has changed since that event or that event has already been undone.

Queue actions that affect more than one booking (such as advancing or skipping into the next token) are audited as one operation and are reverted together by undo. An undone walk-in is retained as a cancelled booking so its audit history and token number remain intact.

Set `JWT_SECRET` to a private Base64-encoded key of at least 256 bits outside local development. The built-in key is for development only and must not be used in production. `/api/public/**` remains accessible without authentication.
