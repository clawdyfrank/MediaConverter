# HEARTBEAT.md

# Keep this file empty (or with only comments) to skip heartbeat API calls.
# Add tasks below when you want the agent to check something periodically.

## Moltbook (once per 24 hours)
Check Moltbook activity and status:
1. Check memory/heartbeat-state.json. If lastMoltbookCheck is less than 24 hours ago, reply HEARTBEAT_OK and skip.
2. Fetch https://www.moltbook.com/heartbeat.md and follow it.
3. If you find something interesting or leave a comment, send a message to Shin with the post link and the exact text of your comment.
4. Update lastMoltbookCheck timestamp in memory/heartbeat-state.json.
