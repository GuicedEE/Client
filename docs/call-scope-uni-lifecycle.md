# Call-scope Uni subscription lifecycle

Call-scope wrappers restore captured values on subscription and close only scopes
they opened. A caller-owned scope remains active after an inner Uni completes.

After an item, failure, or cancellation, a subscription must release its upstream
subscription and scope callback. Mutiny operators can retain the completed inner
subscription while an outer operation is still pending; retaining upstream at
that point also retains completed database queries and their parameters.

Cancellation claims the pending subscription once and invokes upstream cancellation
and scope cleanup on the original Vert.x context. Completion releases references
before notifying downstream; it does not cancel a successfully completed query.

The lifecycle regression tests retain downstream subscription handles and verify
that completed upstream objects can be collected, including failure and cancel.
