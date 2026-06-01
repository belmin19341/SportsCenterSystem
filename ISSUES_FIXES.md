# Issues & Fixes Documentation

---

## Issue #18: Incorrect Error Messages for Authentication and Service Unavailability

**Status:** ✅ FIXED

---

### Problem

1. **Login page:** Wrong credentials showed "Your session is no longer valid" instead of "Invalid username or password"
2. **Dashboard:** Unavailable services (HTTP 503) showed generic "Internal Server Error" instead of a meaningful message

---

### Root Cause

**Frontend (`frontend/src/lib/format.ts`):**
- All 401 errors were treated as session expired, with no distinction for bad credentials
- No handling for HTTP 503 (Service Unavailable)

**Backend (`User Service/GlobalExceptionHandler.java`):**
- `BadCredentialsException` returned generic "Unauthorized" error label

---

### Solution

#### Backend Changes
**File:** `User Service/src/main/java/ba/nwt/userservice/exception/GlobalExceptionHandler.java`

```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
    ApiError error = ApiError.builder()
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("BadCredentials")  // Specific error label
            .message(ex.getMessage() != null ? ex.getMessage() : "Invalid username or password")
            .details(List.of())
            .build();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
}
```

#### Frontend Changes
**File:** `frontend/src/lib/format.ts`

```typescript
if ('status' in error && error.status === 401) {
    const label = 'label' in error ? error.label : undefined
    const message = error.message
    
    // Distinguish bad credentials from expired session
    if (label === 'BadCredentials' || message?.includes('username') || message?.includes('password')) {
        return ['Invalid username or password. Please check your credentials.']
    }
    
    return ['Your session is no longer valid. Sign in again to continue.']
}

// Handle service unavailability
if ('status' in error && error.status === 503) {
    return ['A required service is currently unavailable. Please try again in a few moments.']
}
```

---

### Test Results

✅ Unit tests pass (AuthenticationServiceTest)
✅ Code logic verified and validated
✅ Error flow correctly passes error label from backend to frontend

---

### Files Modified

- `User Service/src/main/java/ba/nwt/userservice/exception/GlobalExceptionHandler.java`
- `frontend/src/lib/format.ts`

---

## Issue #19: Notifications Not Displaying Correctly and Not Auto-Dismissing

**Status:** ✅ FIXED

---

### Problem

1. **Toast notifications:** Success messages stayed on UI until user manually clicked "Dismiss"
2. **Dashboard notifications:** Empty section showed generic "No notifications" message without explanation

---

### Root Cause

**Frontend (`frontend/src/components/feedback.tsx`):**
- `FeedbackBanner` had no auto-dismiss timeout mechanism
- Toast notifications remained indefinitely

**Dashboard (`frontend/src/pages/dashboardPage.tsx`):**
- No distinction between "zero notifications" and "no notifications matching filter"
- Generic empty state message

---

### Solution

#### Auto-Dismiss for Toast Notifications
**File:** `frontend/src/components/feedback.tsx`

Added `useEffect` with 4.5-second auto-dismiss:

```typescript
const AUTO_DISMISS_DELAY_MS = 4500 // 4.5 seconds

export function FeedbackBanner() {
	const {clearFeedback, message} = useFeedback()

	useEffect(() => {
		if (!message) return

		const timeoutId = setTimeout(clearFeedback, AUTO_DISMISS_DELAY_MS)
		return () => clearTimeout(timeoutId)
	}, [message, clearFeedback])

	// ...rest of component
}
```

#### Improved Empty State Messages
**File:** `frontend/src/pages/dashboardPage.tsx`

Differentiated between "no notifications at all" and "no notifications matching filter":

```typescript
: (notificationsQuery.data?.length ?? 0) === 0 ? (
    <div className='text-sm text-slate-400'>
        No notifications yet. You'll see updates about your account and activity here.
    </div>
) : visibleNotifications.length === 0 ? (
    <div className='text-sm text-slate-400'>
        No notifications match the "{notificationFilter.toLowerCase()}" filter.
    </div>
)
```

---

### Files Modified

- `frontend/src/components/feedback.tsx`
- `frontend/src/pages/dashboardPage.tsx`

---

## Issue #20: Incorrect Booking Time Validation

**Status:** ✅ FIXED

---

### Problem

1. **Operating hours bypass:** Users could book times that extend past facility closing time (e.g., start at 23:00, end next day)
2. **Misleading price display:** Price was shown even when the selected time slot was invalid or unavailable

---

### Root Cause

**Frontend validation:**
- `isValidDateRange()` only checked that end time > start time, not that times fit facility hours
- Price quote was fetched and displayed regardless of time validation errors
- Operating hours validation existed but only shown on form submit, not immediately

**Backend:**
- Booking Service did not validate that booking times fall within facility working hours
- No defense against direct API calls that bypass frontend validation

---

### Solution

#### Backend Validation
**File:** `Booking Service/src/main/java/ba/nwt/bookingservice/service/BookingService.java`

Added helper method to validate working hours:

```java
private void validateBookingWithinWorkingHours(FacilityView facility,
                                                LocalDateTime startTime,
                                                LocalDateTime endTime) {
    if (facility == null) {
        throw new IllegalArgumentException("Facility not found");
    }

    if (facility.getWorkingHoursStart() == null || facility.getWorkingHoursEnd() == null) {
        return; // No working hours constraint
    }

    LocalTime start = startTime.toLocalTime();
    LocalTime end = endTime.toLocalTime();
    LocalTime open = facility.getWorkingHoursStart();
    LocalTime close = facility.getWorkingHoursEnd();

    if (start.isBefore(open)) {
        throw new IllegalArgumentException(
            "Booking start time must be after facility opening time (" + open + ")");
    }

    if (end.isAfter(close)) {
        throw new IllegalArgumentException(
            "Booking end time must be before facility closing time (" + close + ")");
    }
}
```

Applied validation in all booking creation/modification endpoints:
- `create(BookingRequestDTO dto)` - new bookings
- `update(Long id, BookingRequestDTO dto)` - update existing bookings
- `patch(Long id, JsonPatch patch)` - partial updates
- `createGroup(BookingRequestDTO bookingDto, List<Long> participantUserIds)` - group bookings
- `createRecurring(BookingRequestDTO base, String pattern, int occurrences)` - recurring bookings
- `createOrchestrated(BookingRequestDTO dto, String paymentMethod)` - orchestrated flow

**File:** `Booking Service/src/main/java/ba/nwt/bookingservice/client/dto/FacilityView.java`

Added working hours fields to FacilityView DTO:

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacilityView {
    private Long id;
    private Long ownerId;
    private String name;
    private String type;
    private String status;
    private LocalTime workingHoursStart;
    private LocalTime workingHoursEnd;
}
```

#### Frontend Improvements
**File:** `frontend/src/lib/validation.ts`

Added `validateBookingTimeOnly()` to check operating hours violations before fetching price:

```typescript
export function validateBookingTimeOnly(input: Omit<BookingValidationInput, 'quote'> & {
	facility: FacilityResponse | null | undefined
}) {
	const errors: string[] = []
	// ... validates start > now, end > start, times within facility hours
	
	if (requestedStart < open || requestedEnd > close) {
		errors.push(`Selected time must fit facility hours (${formatTimeRange(...)})`);
	}
	
	return errors
}
```

**File:** `frontend/src/pages/bookingPage.tsx`

Price quote only displays when time validation errors are zero:

```typescript
{quoteQuery.data && timeValidationErrors.length === 0 ? (
	<Alert variant='success'>
		<div className='font-medium'>
			Quoted total: {formatCurrency(quoteQuery.data.totalPrice)}
		</div>
	</Alert>
) : null}
```

---

### Security Benefits

1. **Defense in depth:** Frontend + backend validation
2. **API protection:** Direct API calls cannot bypass operating hours checks
3. **Clear error messages:** Users understand why booking failed

### Files Modified

**Backend:**
- `Booking Service/src/main/java/ba/nwt/bookingservice/service/BookingService.java` (6 methods updated)
- `Booking Service/src/main/java/ba/nwt/bookingservice/client/dto/FacilityView.java`

**Frontend:**
- `frontend/src/lib/validation.ts`
- `frontend/src/pages/bookingPage.tsx`


Issue #21: Team Evaluator - Loyalty Section Shows Error Instead of Informative Message
Status: ✅ FIXED

Problem
On the Dashboard, for a user who does not have a loyalty category, the message "Could not load data. Loyalty record not found for user id: 1" is displayed. This is a backend message directly shown to the user, appearing as an application failure rather than a valid state. A user who simply hasn't earned loyalty status yet should not see an error.

Root Cause
Frontend (frontend/src/lib/format.ts):

The getErrorMessages function did not specifically handle 404 responses for loyalty records, thus displaying the raw backend error message.
Backend (User Service/src/main/java/ba/nwt/userservice/service/UserLoyaltyService.java):

Throws ResourceNotFoundException when a loyalty record for a user is not found, which is correctly handled by GlobalExceptionHandler to return a 404 status with a descriptive message.
Solution
Frontend Changes
File: frontend/src/lib/format.ts

Added specific handling for 404 errors where the message indicates a missing loyalty record, displaying a user-friendly message:

Typescript

Apply
if ('status' in error && error.status === 404) {
    const message = error.message
    if (message?.includes('Loyalty record not found')) {
        return ['Trenutno ne pripadate nijednoj loyalty kategoriji.']
    }
    return ['Resource not found.']
}


