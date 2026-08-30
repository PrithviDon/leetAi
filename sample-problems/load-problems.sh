#!/bin/bash
# Loads all sample problems into the backend via POST /api/problems.
# Usage: ./load-problems.sh [base_url]
BASE_URL="${1:-http://localhost:8080}"

for file in "$(dirname "$0")"/*.json; do
  name=$(basename "$file")
  echo "Adding $name ..."
  curl -s -X POST "$BASE_URL/api/problems" \
    -H "Content-Type: application/json" \
    -d @"$file" | head -c 200
  echo -e "\n---"
done
