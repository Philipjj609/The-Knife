-- =============================================================================
-- TheKnife Database Schema — PostgreSQL DDL
-- =============================================================================

DROP TABLE IF EXISTS preferiti            CASCADE;
DROP TABLE IF EXISTS ristoranti_servizi   CASCADE;
DROP TABLE IF EXISTS ristoranti_cucine    CASCADE;
DROP TABLE IF EXISTS risposte             CASCADE;
DROP TABLE IF EXISTS recensioni           CASCADE;
DROP TABLE IF EXISTS ristoranti           CASCADE;
DROP TABLE IF EXISTS cucine               CASCADE;
DROP TABLE IF EXISTS servizi              CASCADE;
DROP TABLE IF EXISTS utenti               CASCADE;

DROP TYPE IF EXISTS ruolo_utente;
DROP TYPE IF EXISTS riconoscimento_michelin;

-- =============================================================================
-- TIPI ENUMERATI
-- =============================================================================

CREATE TYPE ruolo_utente AS ENUM (
    'Cliente',
    'Ristoratore'
);

-- NULL sulla colonna = nessun riconoscimento Michelin
CREATE TYPE riconoscimento_michelin AS ENUM (
    '1 Star',
    '2 Stars',
    '3 Stars',
    'Selected Restaurants',
    'Bib Gourmand'
);

-- =============================================================================
-- TABELLA: utenti
-- =============================================================================

CREATE TABLE utenti (
    id            BIGSERIAL      PRIMARY KEY,
    nome          VARCHAR(100)   NOT NULL,
    cognome       VARCHAR(100)   NOT NULL,
    username      VARCHAR(50)    NOT NULL,
    password_hash VARCHAR(255)   NOT NULL,
    data_nascita  DATE,
    domicilio     VARCHAR(255),
    ruolo         ruolo_utente   NOT NULL DEFAULT 'Cliente',

    CONSTRAINT utenti_username_uq   UNIQUE (username),
    CONSTRAINT utenti_nome_check    CHECK  (LENGTH(TRIM(nome))    > 0),
    CONSTRAINT utenti_cognome_check CHECK  (LENGTH(TRIM(cognome)) > 0)
);

-- =============================================================================
-- TABELLA: cucine
-- Lookup dei tipi di cucina (es. "Creative", "Contemporary", "French").
-- =============================================================================

CREATE TABLE cucine (
    id   BIGSERIAL    PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,

    CONSTRAINT cucine_nome_uq    UNIQUE (nome),
    CONSTRAINT cucine_nome_check CHECK  (LENGTH(TRIM(nome)) > 0)
);

-- =============================================================================
-- TABELLA: servizi
-- Lookup dei servizi/facilities (es. "Air conditioning", "Terrace").
-- =============================================================================

CREATE TABLE servizi (
    id   BIGSERIAL    PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,

    CONSTRAINT servizi_nome_uq    UNIQUE (nome),
    CONSTRAINT servizi_nome_check CHECK  (LENGTH(TRIM(nome)) > 0)
);

-- =============================================================================
-- TABELLA: ristoranti
-- prezzo_livello 1–4 (1=€, 2=€€, 3=€€€, 4=€€€€) — agnostico dalla valuta.
-- proprietario_id: NULL = ristorante del catalogo senza gestore registrato.
-- =============================================================================

CREATE TABLE ristoranti (
    id                  BIGSERIAL              PRIMARY KEY,
    nome                VARCHAR(255)           NOT NULL,
    indirizzo           VARCHAR(500),
    citta               VARCHAR(150),
    nazione             VARCHAR(100),
    latitudine          NUMERIC(10, 7),
    longitudine         NUMERIC(10, 7),
    prezzo_livello      SMALLINT,
    telefono            VARCHAR(50),
    url                 VARCHAR(1000),
    sito_web            VARCHAR(1000),
    riconoscimento      riconoscimento_michelin,
    green_star          BOOLEAN                NOT NULL DEFAULT FALSE,
    descrizione         TEXT,
    delivery            BOOLEAN                NOT NULL DEFAULT FALSE,
    prenotazione_online BOOLEAN                NOT NULL DEFAULT FALSE,
    proprietario_id     BIGINT,

    CONSTRAINT ristoranti_nome_check    CHECK (LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ristoranti_lat_check     CHECK (latitudine  IS NULL OR latitudine  BETWEEN -90  AND 90),
    CONSTRAINT ristoranti_lon_check     CHECK (longitudine IS NULL OR longitudine BETWEEN -180 AND 180),
    CONSTRAINT ristoranti_prezzo_check  CHECK (prezzo_livello IS NULL OR prezzo_livello BETWEEN 1 AND 4),
    CONSTRAINT ristoranti_nome_ind_uq   UNIQUE (nome, indirizzo),

    CONSTRAINT ristoranti_proprietario_fk FOREIGN KEY (proprietario_id)
        REFERENCES utenti (id)
        ON DELETE SET NULL
);

-- =============================================================================
-- TABELLA DI GIUNZIONE: ristoranti_cucine  (N:M ristoranti ↔ cucine)
-- =============================================================================

CREATE TABLE ristoranti_cucine (
    ristorante_id BIGINT NOT NULL,
    cucina_id     BIGINT NOT NULL,

    PRIMARY KEY (ristorante_id, cucina_id),

    CONSTRAINT rc_ristorante_fk FOREIGN KEY (ristorante_id)
        REFERENCES ristoranti (id)
        ON DELETE CASCADE,

    CONSTRAINT rc_cucina_fk     FOREIGN KEY (cucina_id)
        REFERENCES cucine (id)
        ON DELETE CASCADE
);

-- =============================================================================
-- TABELLA DI GIUNZIONE: ristoranti_servizi  (N:M ristoranti ↔ servizi)
-- =============================================================================

CREATE TABLE ristoranti_servizi (
    ristorante_id BIGINT NOT NULL,
    servizio_id   BIGINT NOT NULL,

    PRIMARY KEY (ristorante_id, servizio_id),

    CONSTRAINT rs_ristorante_fk FOREIGN KEY (ristorante_id)
        REFERENCES ristoranti (id)
        ON DELETE CASCADE,

    CONSTRAINT rs_servizio_fk   FOREIGN KEY (servizio_id)
        REFERENCES servizi (id)
        ON DELETE CASCADE
);

-- =============================================================================
-- TABELLA: recensioni
-- Un cliente può lasciare al massimo una recensione per ristorante.
-- =============================================================================

CREATE TABLE recensioni (
    id               BIGSERIAL   PRIMARY KEY,
    username_cliente VARCHAR(50) NOT NULL,
    ristorante_id    BIGINT      NOT NULL,
    valutazione      SMALLINT    NOT NULL,
    titolo           VARCHAR(255),
    commento         TEXT,
    data_recensione  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT rec_valutazione_check CHECK  (valutazione BETWEEN 1 AND 5),
    CONSTRAINT rec_cliente_rist_uq   UNIQUE (username_cliente, ristorante_id),

    CONSTRAINT rec_cliente_fk        FOREIGN KEY (username_cliente)
        REFERENCES utenti (username)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT rec_ristorante_fk     FOREIGN KEY (ristorante_id)
        REFERENCES ristoranti (id)
        ON DELETE CASCADE
);

-- =============================================================================
-- TABELLA: risposte
-- Max una risposta per recensione (UNIQUE su recensione_id).
-- =============================================================================

CREATE TABLE risposte (
    id                   BIGSERIAL   PRIMARY KEY,
    recensione_id        BIGINT      NOT NULL,
    username_ristoratore VARCHAR(50) NOT NULL,
    testo                TEXT        NOT NULL,
    data_risposta        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT risp_recensione_uq  UNIQUE (recensione_id),
    CONSTRAINT risp_testo_check    CHECK  (LENGTH(TRIM(testo)) > 0),

    CONSTRAINT risp_recensione_fk  FOREIGN KEY (recensione_id)
        REFERENCES recensioni (id)
        ON DELETE CASCADE,

    CONSTRAINT risp_ristoratore_fk FOREIGN KEY (username_ristoratore)
        REFERENCES utenti (username)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- =============================================================================
-- TABELLA: preferiti  (N:M utenti ↔ ristoranti)
-- =============================================================================

CREATE TABLE preferiti (
    username      VARCHAR(50) NOT NULL,
    ristorante_id BIGINT      NOT NULL,
    data_aggiunta TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (username, ristorante_id),

    CONSTRAINT pref_utente_fk     FOREIGN KEY (username)
        REFERENCES utenti (username)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT pref_ristorante_fk FOREIGN KEY (ristorante_id)
        REFERENCES ristoranti (id)
        ON DELETE CASCADE
);

-- =============================================================================
-- INDICI
-- =============================================================================

CREATE INDEX idx_ristoranti_nome           ON ristoranti        (LOWER(nome));
CREATE INDEX idx_ristoranti_citta          ON ristoranti        (LOWER(citta));
CREATE INDEX idx_ristoranti_nazione        ON ristoranti        (LOWER(nazione));
CREATE INDEX idx_ristoranti_prezzo         ON ristoranti        (prezzo_livello);
CREATE INDEX idx_ristoranti_riconoscimento ON ristoranti        (riconoscimento);
CREATE INDEX idx_ristoranti_proprietario   ON ristoranti        (proprietario_id);

CREATE INDEX idx_rc_cucina                 ON ristoranti_cucine (cucina_id);
CREATE INDEX idx_rs_servizio               ON ristoranti_servizi(servizio_id);

CREATE INDEX idx_recensioni_ristorante     ON recensioni        (ristorante_id);
CREATE INDEX idx_recensioni_cliente        ON recensioni        (username_cliente);

CREATE INDEX idx_risposte_recensione       ON risposte          (recensione_id);

CREATE INDEX idx_preferiti_username        ON preferiti         (username);
