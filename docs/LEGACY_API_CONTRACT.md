# Legacy Kitchensink API Contract

## Base URL
`http://localhost:8080/kitchensink/rest`

## Endpoints

### 1. List All Members
```
GET /members
Response: 200 OK
[
  {
    "id": 0,
    "name": "John Smith",
    "email": "john.smith@mailinator.com",
    "phoneNumber": "2125551212"
  }
]
```

### 2. Get Member by ID
```
GET /members/{id}
Response: 200 OK
{
  "id": 0,
  "name": "John Smith",
  "email": "john.smith@mailinator.com",
  "phoneNumber": "2125551212"
}

Response: 404 Not Found (if member doesn't exist)
```

### 3. Create Member
```
POST /members
Content-Type: application/json

Request Body:
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "phoneNumber": "1234567890"
}

Response: 200 OK (empty body on success)

Validation Errors: 400 Bad Request
{
  "email": "Email taken",
  "name": "Must not contain numbers"
}
```

## Validation Rules

| Field | Rules |
|-------|-------|
| name | Required, 1-25 chars, no numbers |
| email | Required, valid email format, unique |
| phoneNumber | Required, 10-12 digits only |

## Business Rules

1. Email must be unique across all members
2. Members are returned sorted by name (ascending)
3. ID is auto-generated (Long in legacy, will be String/ObjectId in MongoDB)

