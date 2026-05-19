\set ON_ERROR_STOP on

SET client_encoding = 'UTF8';

CREATE TEMP TABLE import_michelin (
    name                        TEXT,
    address                     TEXT,
    location                    TEXT,
    price                       TEXT,
    cuisine                     TEXT,
    longitude                   TEXT,
    latitude                    TEXT,
    phone_number                TEXT,
    url                         TEXT,
    website_url                 TEXT,
    award                       TEXT,
    green_star                  TEXT,
    facilities_and_services     TEXT,
    description                 TEXT,
    delivery_available          TEXT,
    online_booking_available    TEXT
);

\copy import_michelin FROM '__CSV_PATH__' WITH (FORMAT csv, HEADER true)

BEGIN;

WITH normalized AS (
    SELECT DISTINCT ON (btrim(name), NULLIF(btrim(address), ''))
           btrim(name) AS nome,
           NULLIF(btrim(address), '') AS indirizzo,
           NULLIF(btrim(split_part(location, ',', 1)), '') AS citta,
           CASE
               WHEN position(',' IN location) > 0 THEN NULLIF(btrim(regexp_replace(location, '^.*,', '')), '')
               ELSE NULL
           END AS nazione,
           CASE WHEN btrim(latitude)  ~ '^-?[0-9]+(\.[0-9]+)?$' THEN btrim(latitude)::numeric  ELSE NULL END AS latitudine,
           CASE WHEN btrim(longitude) ~ '^-?[0-9]+(\.[0-9]+)?$' THEN btrim(longitude)::numeric ELSE NULL END AS longitudine,
           CASE
               WHEN NULLIF(btrim(price), '') IS NULL THEN NULL
               ELSE LEAST(char_length(btrim(price)), 4)::smallint
           END AS prezzo_livello,
           NULLIF(btrim(phone_number), '') AS telefono,
           NULLIF(btrim(url), '') AS url,
           NULLIF(btrim(website_url), '') AS sito_web,
           CASE
               WHEN btrim(award) IN ('1 Star', '2 Stars', '3 Stars', 'Selected Restaurants', 'Bib Gourmand')
                   THEN btrim(award)::riconoscimento_michelin
               ELSE NULL
           END AS riconoscimento,
           lower(btrim(green_star)) IN ('1', 'true', 't', 'yes', 'y', 'si', 'sì', 'sã¬') AS green_star,
           NULLIF(btrim(description), '') AS descrizione,
           lower(btrim(delivery_available)) IN ('1', 'true', 't', 'yes', 'y', 'si', 'sì', 'sã¬') AS delivery,
           lower(btrim(online_booking_available)) IN ('1', 'true', 't', 'yes', 'y', 'si', 'sì', 'sã¬') AS prenotazione_online
    FROM import_michelin
    WHERE NULLIF(btrim(name), '') IS NOT NULL
    ORDER BY btrim(name), NULLIF(btrim(address), '')
)
INSERT INTO ristoranti (
    nome, indirizzo, citta, nazione, latitudine, longitudine, prezzo_livello,
    telefono, url, sito_web, riconoscimento, green_star, descrizione,
    delivery, prenotazione_online
)
SELECT nome, indirizzo, citta, nazione, latitudine, longitudine, prezzo_livello,
       telefono, url, sito_web, riconoscimento, green_star, descrizione,
       delivery, prenotazione_online
FROM normalized
ON CONFLICT (nome, indirizzo) DO UPDATE SET
    citta = EXCLUDED.citta,
    nazione = EXCLUDED.nazione,
    latitudine = EXCLUDED.latitudine,
    longitudine = EXCLUDED.longitudine,
    prezzo_livello = EXCLUDED.prezzo_livello,
    telefono = EXCLUDED.telefono,
    url = EXCLUDED.url,
    sito_web = EXCLUDED.sito_web,
    riconoscimento = EXCLUDED.riconoscimento,
    green_star = EXCLUDED.green_star,
    descrizione = EXCLUDED.descrizione,
    delivery = EXCLUDED.delivery,
    prenotazione_online = EXCLUDED.prenotazione_online;

WITH cuisine_values AS (
    SELECT DISTINCT btrim(value) AS nome
    FROM import_michelin,
         regexp_split_to_table(coalesce(cuisine, ''), '\s*,\s*') AS value
    WHERE NULLIF(btrim(value), '') IS NOT NULL
)
INSERT INTO cucine (nome)
SELECT nome FROM cuisine_values
ON CONFLICT (nome) DO NOTHING;

WITH service_values AS (
    SELECT DISTINCT btrim(value) AS nome
    FROM import_michelin,
         regexp_split_to_table(coalesce(facilities_and_services, ''), '\s*,\s*') AS value
    WHERE NULLIF(btrim(value), '') IS NOT NULL
)
INSERT INTO servizi (nome)
SELECT nome FROM service_values
ON CONFLICT (nome) DO NOTHING;

WITH source AS (
    SELECT DISTINCT
           btrim(name) AS nome,
           NULLIF(btrim(address), '') AS indirizzo,
           btrim(value) AS cucina
    FROM import_michelin,
         regexp_split_to_table(coalesce(cuisine, ''), '\s*,\s*') AS value
    WHERE NULLIF(btrim(name), '') IS NOT NULL
      AND NULLIF(btrim(value), '') IS NOT NULL
)
INSERT INTO ristoranti_cucine (ristorante_id, cucina_id)
SELECT r.id, c.id
FROM source s
JOIN ristoranti r
  ON r.nome = s.nome
 AND r.indirizzo IS NOT DISTINCT FROM s.indirizzo
JOIN cucine c ON c.nome = s.cucina
ON CONFLICT DO NOTHING;

WITH source AS (
    SELECT DISTINCT
           btrim(name) AS nome,
           NULLIF(btrim(address), '') AS indirizzo,
           btrim(value) AS servizio
    FROM import_michelin,
         regexp_split_to_table(coalesce(facilities_and_services, ''), '\s*,\s*') AS value
    WHERE NULLIF(btrim(name), '') IS NOT NULL
      AND NULLIF(btrim(value), '') IS NOT NULL
)
INSERT INTO ristoranti_servizi (ristorante_id, servizio_id)
SELECT r.id, s.id
FROM source src
JOIN ristoranti r
  ON r.nome = src.nome
 AND r.indirizzo IS NOT DISTINCT FROM src.indirizzo
JOIN servizi s ON s.nome = src.servizio
ON CONFLICT DO NOTHING;

COMMIT;

SELECT
    (SELECT count(*) FROM ristoranti) AS ristoranti,
    (SELECT count(*) FROM cucine) AS cucine,
    (SELECT count(*) FROM servizi) AS servizi,
    (SELECT count(*) FROM ristoranti_cucine) AS associazioni_cucine,
    (SELECT count(*) FROM ristoranti_servizi) AS associazioni_servizi;
