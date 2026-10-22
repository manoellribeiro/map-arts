---
name: guided-change
description: "Guided, step-by-step workflow for refactoring code or implementing a feature in the Martp android open source project while the user learns. Use when the user wants to analyse some code (or plan a feature), get a numbered list of problems or steps, and work through them one at a time: each item explained as a written tutorial (concept, then before/after code), applied only after the user says so, then tested by the user. Not for one-shot changes the user just wants done."
---

The user drives this workflow and learns from it. You analyse, explain and apply; the user decides, tests and commits. Never jump ahead to a later item or change code the user has not approved.

## 1. Start

### 1.1 Understand the target
- Read the target code (a class, a screen, a package, or the feature request) and everything it depends on that matters for the topic: ViewModels, models, repositories, build files.
- Find the domain skill that applies and load it before analysing (e.g. `jetpack-compose-performance` for Compose performance, `unit-test` for tests). When that skill says to read its references before flagging something, read them; never rely on its summary bullets alone.
- Check the versions that limit the possible fixes (Compose BOM, Kotlin, AGP, libraries) and say when a fix needs a newer version than the project has.
- If no installed skill covers the topic, say so before starting.

### 1.2 Build the numbered list
- **Refactor**: list every problem found, numbered, each with one line on what is wrong, why it matters and the expected gain. Order them so earlier fixes don't get rewritten by later ones, and say how severe each one is.
- **New feature**: list the implementation steps, numbered, each small enough to be explained, applied and tested on its own.
- Add a **Setup** group before the list when the work needs prerequisites (dependency updates, build flags, a "before" measurement), and a **Later (optional)** group for related work that is out of scope.
- Do not give code yet. The user picks the items, usually in order ("let's do the 3").

### 1.3 Measure before changing (when it applies)
For performance work, help the user record a "before" baseline first (e.g. Layout Inspector recomposition counts, a release build, a benchmark), with exact scenarios the user can repeat after each fix. The user runs the measurements and shares the results (often as screenshots); compare against them after each fix.

## 2. Each item: the written tutorial

When the user asks for an item, answer in this format and stop. Do not edit any file yet.

1. **The concept**: the idea behind the problem in plain words, with a small example when it helps. Explain the general rule, not only this code.
2. **The problem in this code**: point at the code by class and method name, never by line number, and show what goes wrong (e.g. what recomposes, what allocates, which bug happens).
3. **The change, step by step**: before/after code for each step, small enough to read. Show every place that changes, including imports and callers.
4. **Why it works**: tie the change back to the concept.
5. **How to verify**: the manual tests and, when it applies, the measurement scenarios with the expected result.

Then wait. The user may ask questions, disagree, or suggest a different solution. Answer the questions; when the user proposes another approach, evaluate it honestly and use it if it works.

Some items end with "keep the code as it is". Say so clearly with the reason; teaching why the current code is right is still a finished item.

## 3. Apply only when the user says so

- Edit the code only after an explicit "apply it" / "you can apply" for that item. Approval for one item never covers the next.
- Re-read the current file before editing: the user may have committed or changed it since the tutorial. Apply the tutorial's change to the code as it is now.
- Build after editing (`sh gradlew :app:assembleDebug`; `./gradlew` has no execute permission here) and report the result honestly, including warnings that matter. Say plainly that you built it but did not run it on a device.
- Summarise what changed, then give the manual tests to run.
- The item is done only when the user says it was tested.

## 4. After every finished item

End the reply with the updated **Remaining tasks** checklist so the user never needs to scroll back: the Setup group, the numbered items (done ones marked, with a one-line result such as a measured gain), and the Later (optional) group.
- Mark an item as "Applied ✅, still to do: your manual test" until the user confirms the test.
- Never include commit steps or commit reminders. The user commits on their own.

## 5. Findings outside the scope

Bugs or issues you find that are not part of the current list (e.g. a bug in a ViewModel while refactoring the UI):
- Tell the user, and say whether the finding is verified or only inferred from reading the code. If it is inferred, give the user a way to confirm it.
- Ask whether to fix it now, add it to the list, or record it where the user tracks bugs. Never post anywhere outside the repo without showing the exact text and getting an explicit yes, every time.

## 6. Code rules

- Write code that matches the surrounding code: naming, idioms and comment density.
- Don't write code comments that repeat what the tutorial already explained. Only keep comments that carry information the code and the conversation don't.
- Remove imports that a change leaves unused.
- Point at code by class and method name in all explanations.
