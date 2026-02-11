# Docker Deployment Guide

This guide explains how to deploy the IDVerse application using Docker Compose with a permanent MySQL database.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+

## Quick Start

1. **Copy the environment file template:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` file** with your preferred settings (optional - defaults will work):
   ```bash
   # MySQL Configuration
   MYSQL_ROOT_PASSWORD=rootpassword
   MYSQL_DATABASE=idverse
   MYSQL_USER=idverse_user
   MYSQL_PASSWORD=idverse_pass
   MYSQL_PORT=3306

   # Application Configuration
   APP_PORT=19746
   DDL_AUTO=update
   SHOW_SQL=false
   ```

3. **Start the services:**
   ```bash
   docker-compose up -d
   ```

4. **Check the logs:**
   ```bash
   # All services
   docker-compose logs -f

   # Just the application
   docker-compose logs -f app

   # Just MySQL
   docker-compose logs -f mysql
   ```

5. **Access the application:**
   - Application: http://localhost:19746
   - The application will automatically connect to MySQL

## Common Commands

### Start services
```bash
docker-compose up -d
```

### Stop services (keeps data)
```bash
docker-compose down
```

### Stop services and remove volumes (DELETES DATA)
```bash
docker-compose down -v
```

### Rebuild the application after code changes
```bash
docker-compose up -d --build app
```

### View logs
```bash
docker-compose logs -f app
```

### Access MySQL database
```bash
docker-compose exec mysql mysql -u idverse_user -p
# Enter password: idverse_pass (or your custom password from .env)
```

### Restart a specific service
```bash
docker-compose restart app
```

## Data Persistence

The MySQL data is stored in a Docker named volume called `mysql_data`. This means:
- ✅ Data persists even when containers are stopped or removed
- ✅ Data survives `docker-compose down`
- ❌ Data is deleted with `docker-compose down -v`

### Backup MySQL Data

```bash
# Create a backup
docker-compose exec mysql mysqldump -u root -p idverse > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
docker-compose exec -T mysql mysql -u root -p idverse < backup_20240101_120000.sql
```

## Custom MySQL Configuration

You can add custom MySQL configuration files:

1. Create the directory:
   ```bash
   mkdir -p docker/mysql/conf.d
   ```

2. Add your `.cnf` files to `docker/mysql/conf.d/`

3. Restart MySQL:
   ```bash
   docker-compose restart mysql
   ```

## Initialization Scripts

To run SQL scripts on first MySQL startup:

1. Create the directory:
   ```bash
   mkdir -p docker/mysql/init
   ```

2. Add your `.sql` files to `docker/mysql/init/`

3. They will run automatically on first container creation

## Troubleshooting

### Application can't connect to MySQL

1. Check if MySQL is healthy:
   ```bash
   docker-compose ps
   ```

2. Check MySQL logs:
   ```bash
   docker-compose logs mysql
   ```

3. Verify the database was created:
   ```bash
   docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
   ```

### Port already in use

If port 19746 or 3306 is already in use, change it in `.env`:
```bash
APP_PORT=8080
MYSQL_PORT=3307
```

### Reset everything

To completely reset (WARNING: deletes all data):
```bash
docker-compose down -v
docker-compose up -d
```

### View application environment variables

```bash
docker-compose exec app env | grep SPRING
```

## Production Considerations

For production deployments:

1. **Change default passwords** in `.env`
2. **Use secrets management** instead of `.env` file
3. **Set `DDL_AUTO=validate`** to prevent automatic schema changes
4. **Enable SSL** for MySQL connections
5. **Set up regular backups** of MySQL data
6. **Use resource limits** in docker-compose.yml:
   ```yaml
   app:
     deploy:
       resources:
         limits:
           cpus: '1.0'
           memory: 1G
   ```
7. **Consider using external MySQL** instead of containerized database
8. **Set up monitoring and health checks**

## Docker Profiles Explained

The application uses Spring profiles:
- **Default profile**: Uses H2 in-memory database (development)
- **`docker` profile**: Uses MySQL database (configured in `application-docker.yml`)

The `docker` profile is automatically activated when running via Docker Compose.

## Network

Services communicate over a dedicated bridge network called `idverse-network`. The application connects to MySQL using the hostname `mysql`.
