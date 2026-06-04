# Frontend Development Guidelines

> Best practices for frontend development in this project.

---

## Overview

Conventions for the Vue 3 + TypeScript + Element Plus + Pinia frontend (`frontend/`).
Stack facts: plain CSS (no Sass), Vite build with `vue-tsc` type-check, in-house `AppIcon` (no icon library).

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | Drafted |
| [Component Guidelines](./component-guidelines.md) | Component patterns, props, composition | Drafted |
| [Styling & Design Tokens](./styling.md) | CSS tokens, theming, glassmorphism | Drafted |
| [Hook Guidelines](./hook-guidelines.md) | Composables, data fetching patterns | To fill |
| [State Management](./state-management.md) | Local state, global state, server state | To fill |
| [Quality Guidelines](./quality-guidelines.md) | Code standards, forbidden patterns | Drafted |
| [Type Safety](./type-safety.md) | Type patterns, validation | To fill |

---

## How to Fill These Guidelines

For each guideline file:

1. Document your project's **actual conventions** (not ideals)
2. Include **code examples** from your codebase
3. List **forbidden patterns** and why
4. Add **common mistakes** your team has made

The goal is to help AI assistants and new team members understand how YOUR project works.

---

**Language**: All documentation should be written in **English**.
