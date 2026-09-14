# Owned-world editing (unreleased)

## Protocol

The implementation targets Axiom API 10: server challenge on `axiom:hello`, exact protocol/data
version and single-use nonce response, ServerConfig v0 in `axiom:enable`, and fragmented optionally
Zstd-compressed `axiom:tunnel` requests. It does not retain the older API 9 handshake or guess
state IDs across Minecraft versions. Unknown/incompatible clients get a rejection, not guessed
block mappings. Client channel registration is required for outgoing custom-block definitions.

Primary wire references inspected on 2026-09-14:

- [Protocol rework](https://github.com/Moulberry/AxiomPaperPlugin/commit/8978ffb0bfd1b29672ae100db372fe79811706d0)
- [Handshake and tunnel](https://github.com/Moulberry/AxiomPaperPlugin/blob/master/src/main/java/com/moulberry/axiom/AxiomPaper.java)
- [Block/biome request](https://github.com/Moulberry/AxiomPaperPlugin/blob/master/src/main/java/com/moulberry/axiom/packet/impl/SetBlockBufferPacketListener.java)
- [Biome byte-map coordinates](https://github.com/Moulberry/AxiomPaperPlugin/blob/master/src/main/java/com/moulberry/axiom/buffer/Position2ByteMap.java)
- [Custom-block v2 encoding](https://github.com/Moulberry/AxiomPaperPlugin/blob/master/src/main/java/com/moulberry/axiom/paperapi/block/ImplAxiomCustomBlock.java)

## Ownership seam

Install `AxiomEdits.setTargetResolver` at server startup. It captures an `EditTarget` from the
player's **actual instance** before asynchronous decoding. Return null to reject; never fall back
to the ordinary writer for an owned world. `EditTarget.apply` must use the world's serialized
owner, bound its per-tick work, and check `Request.requireCurrent()` before each slice. The returned
stage completes only after writes and client corrections settle. Failure after an earlier slice
is explicitly partial, not a transaction rollback.

The default `ScheduledEditTarget` retains ordinary Minestom placement/break events for single
edits and exact terrain/NBT/biome writes for bulk edits. It requires loaded chunks and performs
512 cell visits per tick. Workspaces replace this target with ownership-aware custom-block writes;
they must not dispatch the ordinary placement event and then apply the same edit a second time.

Install `AxiomEdits.setChannelPolicy` too: it is consulted before **every** decoded inner or direct
Axiom operation. A workspace must explicitly constrain entity/NBT/teleport/annotation channels,
not merely block placement. The default policy preserves separately configured ordinary-server
permissions; it is not a staff authorization policy. Transfers/disconnects/handshakes invalidate
queued requests. Catalogue changes invalidate them as well. Permission revocation is checked at
application time, including NBT permission previously granted at capture.

## Bounded work

- 2 MiB wire payload, 32 sections, 256 KiB per block-entity NBT, 4 MiB total decoded NBT/request.
- Full strict decode before world mutation: sentinel, dispatch-credit field, palettes, coordinates,
  section-local NBT and trailing data. No successful partial parse or silent malformed-NBT fallback.
- Compact immutable sections rather than one retained object per cell. Biome byte-map coordinates
  are quart positions, not Minecraft block sections.
- Four edit requests globally, one in flight per player, with the permit held through application.
  One platform decode worker; no common-pool live mutations or blocking per-player semaphore wait.
- Initial/replenished dispatch credit is one section. This is **flow control, not success evidence**:
  terminal failures replenish the credit and separately report an error. Pilot client throughput
  and behavior when an operation spans several buffers still require human testing.
- Four incomplete tunnel assemblies (max 128 fragments, ten seconds, bounded total bytes per
  assembly), plus four decoding/delivery slots. Fragment state is cleared on player transfer.
  Compressed packets decode off the tick path; stale decoded deliveries are rejected.

These are adjustable conservative pilot limits, not a performance or no-loss guarantee. A host
must also bound chunk loading, logical entities, disk use and its own edit admission.

## Custom blocks

`CustomBlockCatalogue` owns a generation and unique exact carrier states. Definitions encode finite
properties, optional pick-block items, explicit placement roles and transform maps. Transform
closure and inverse/cycle checks reject inconsistent definitions. Vanilla carrier auto-rotation is
disabled. The host must generate the state list and transforms from its logical registry/allocation,
not infer ownership from arbitrary vanilla terrain.

Install `CustomBlockCatalogues.setProvider`, returning EMPTY until the correct pack is ready and
the player may edit that world. Call `refresh` on pack-ready changes (handshake/world changes are
already covered). Clear/resend and session invalidation happen on generation changes. A generation
must not be reused for changed definitions. Display-backed objects sharing barrier/air hosts do
not belong in this state-backed palette; protect and edit them through their logical object owner.

## Verification status

30 local tests pass: existing channel/marker/teleport behavior, terrain/NBT regression cases,
strict malformed/budget checks, biome coordinate decoding, bounded application cancellation,
captured session and permission revocation, cancellation-shielded admission, tunnel reassembly
and expansion limits, API 10 nonce/layout, custom-block item/transform encoding and bounded
polymorphic annotation round-trips (including annotation-first static initialization).

Still required before activation: real API 10 client handshake, property Cartesian ordering,
custom palette/pick/rotate/mirror/copy/undo, fragment backpressure across large pastes, and host
integration. No release, deployment, or Paper retirement is implied by these local tests.

## Owned annotations

`AxiomAnnotationUpdateEvent` is cancellable before ordinary annotation storage is selected.
An owned host cancels it, captures the player/world/session, and applies its actions through its
serialized mutation queue with permission and pack checks repeated at execution. Merely installing
an annotation store does not serialize network mutations.

Preload `AnnotationManager.create(stableWorldId, storage, config)` off tick, then `bind` it to the
live instance through `AnnotationRegistry` before admitting players. The registry will not replace
an existing manager. Remove that binding after the host's final flush. The host store must propagate
validation errors: rejected batches restore the manager's prior state and are not broadcast.
Unsupported transforms leave their objects unchanged. Full-world sends respect the 256-action
packet limit, including the initial clear action.

`AnnotationJsonCodec` preserves every wire annotation type with an explicit discriminator, validates
required wire fields and finite numbers, and bounds counts and aggregate encoded size. Unknown or
malformed data fails visibly; it does not turn into an empty map. A host may buffer accepted snapshots
and flush them on its existing bounded I/O worker. Axiom's ordinary per-instance JSON storage is
not a durable workspace lifecycle implementation and must not be used as a workspace fallback.
