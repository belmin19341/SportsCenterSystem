# Docker izvjestaj

Docker okruzenje pokriva trazene stavke:

- Dockerfile postoji za frontend, config-server, discovery-server i sve mikroservise.
- Dockerfile postoji i za svaku MySQL bazu, uz mali `low-memory.cnf`.
- `docker-compose.yml` pokrece baze, RabbitMQ, config/discovery servere, mikroservise, API gateway i frontend.
- Interna komunikacija koristi Docker DNS imena kao `mysql-user`, `rabbitmq`, `config-server`, `discovery-server`, `user-service`.
- Kontejneri imaju osnovne optimizacije: multi-stage/layered Java image, nginx frontend, MySQL low-memory config i compose `mem_limit`.

Pokretanje:

```bash
docker compose up --build
```
