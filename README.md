# amiblocked-server
![Continous Integration](https://github.com/GiantTreeLP/amiblocked-server/workflows/Continous%20Integration/badge.svg)

API backend server for my "Am I blocked?" website.

## API Endpoints

### POST `/api/v1/find`

Parameters: 
    
-   `search`: String to search for. Will be used as the username with and without a discriminator, as well as the snowflake.

Response: A JSON-encoded [`BlockedUserDTO`](src/main/kotlin/BlockedUser.kt)
