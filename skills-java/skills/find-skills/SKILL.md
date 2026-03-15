---
name: find-skills
description: Helps discover and evaluate skills from the skills ecosystem
---

# Find Skills

## Overview

This skill helps you find, evaluate, and install skills from the open agent skills ecosystem at [skills.sh](https://skills.sh).

## Instructions

When a user asks about extending your capabilities, finding agent skills, or wants to discover reusable instruction sets:

1. Use `skills find [query]` to search for relevant skills
2. Show the user a curated list of results with names, descriptions, and install counts
3. Explain how to install a skill with `skills add <source>`
4. For Java/JBang users, note they can use `jbang skills@jbangdev/skills-java find [query]`

## Examples

- "What skills are available for code review?" → `skills find code review`
- "Find web design skills" → `skills find web-design`
- "Show me all available skills" → `skills find`
