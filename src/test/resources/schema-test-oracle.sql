-- ===== SEQUENCES: drop-if-exists =====
BEGIN
EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_MEMBER_PRODUCT';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -2289 THEN RAISE; END IF; -- -2289: sequence does not exist
END;
/
BEGIN
EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_MEMBER_PRODUCT_EFFECT';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -2289 THEN RAISE; END IF;
END;
/

-- ===== TABLES: drop-if-exists (필요 테이블 전부) =====
BEGIN
EXECUTE IMMEDIATE 'DROP TABLE MEMBER_PRODUCT_EFFECT CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 THEN RAISE; END IF; -- -942: table not exists
END;
/
BEGIN
EXECUTE IMMEDIATE 'DROP TABLE MEMBER_PRODUCT CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
EXECUTE IMMEDIATE 'DROP TABLE PRODUCT_EFFECT CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
EXECUTE IMMEDIATE 'DROP TABLE PRODUCT CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
EXECUTE IMMEDIATE 'DROP TABLE EFFECT CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
EXECUTE IMMEDIATE 'DROP TABLE CATEGORY CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

-- ===== SEQUENCES: re-create =====
CREATE SEQUENCE SEQ_MEMBER_PRODUCT START WITH 1 INCREMENT BY 1
    /
CREATE SEQUENCE SEQ_MEMBER_PRODUCT_EFFECT START WITH 1 INCREMENT BY 1
    /

-- ===== TABLES: re-create =====
CREATE TABLE CATEGORY (
                          CATEGORY_ID   NUMBER        PRIMARY KEY,
                          CATEGORY_NAME VARCHAR2(100) NOT NULL,
                          DELETED_AT    TIMESTAMP     NULL
)
    /

CREATE TABLE EFFECT (
                        EFFECT_ID   NUMBER        PRIMARY KEY,
                        EFFECT_NAME VARCHAR2(200) NOT NULL,
                        DELETED_AT  TIMESTAMP     NULL
)
    /

CREATE TABLE PRODUCT (
                         PRODUCT_ID         NUMBER        PRIMARY KEY,
                         PRODUCT_NAME       VARCHAR2(200) NOT NULL,
                         BRAND_NAME         VARCHAR2(200) NOT NULL,
                         RECOMMENDED_PERIOD NUMBER        NOT NULL,
                         BH_TYPE            NUMBER        NOT NULL,
                         CATEGORY_ID        NUMBER        NOT NULL,
                         DELETED_AT         TIMESTAMP     NULL
)
    /

CREATE TABLE PRODUCT_EFFECT (
                                PRODUCT_ID NUMBER NOT NULL,
                                EFFECT_ID  NUMBER NOT NULL
)
    /

CREATE TABLE MEMBER_PRODUCT (
                                MEMBER_PRODUCT_ID           NUMBER        PRIMARY KEY,
                                MEMBER_ID                   NUMBER        NOT NULL,
                                PRODUCT_ID                  NUMBER        NULL,
                                START_DATE                  DATE          NOT NULL,
                                RECOMMENDED_EXPIRATION_DATE NUMBER        NOT NULL,
                                USAGE_STATUS                NUMBER        NOT NULL,
                                PRODUCT_NAME                VARCHAR2(200) NOT NULL,
                                BRAND_NAME                  VARCHAR2(200) NOT NULL,
                                COMPLETED_DATE              DATE          NULL,
                                BH_TYPE                     NUMBER        NOT NULL,
                                CATEGORY_ID                 NUMBER        NULL,
                                CREATED_AT                  TIMESTAMP     DEFAULT SYSTIMESTAMP,
                                CREATED_BY                  NUMBER,
                                UPDATED_AT                  TIMESTAMP     DEFAULT SYSTIMESTAMP,
                                UPDATED_BY                  NUMBER,
                                DELETED_AT                  TIMESTAMP     NULL,
                                DELETED_BY                  NUMBER
)
    /

CREATE TABLE MEMBER_PRODUCT_EFFECT (
                                       MEMBER_PRODUCT_EFFECT_ID NUMBER     PRIMARY KEY,
                                       MEMBER_PRODUCT_ID        NUMBER     NOT NULL,
                                       EFFECT_ID                NUMBER     NOT NULL,
                                       CREATED_AT               TIMESTAMP  DEFAULT SYSTIMESTAMP,
                                       CREATED_BY               NUMBER,
                                       UPDATED_AT               TIMESTAMP  DEFAULT SYSTIMESTAMP,
                                       UPDATED_BY               NUMBER
)
    /

-- ===== INDEXES =====
CREATE INDEX IDX_MP_MEMBER ON MEMBER_PRODUCT (MEMBER_ID)
    /
CREATE INDEX IDX_MP_PRODUCT ON MEMBER_PRODUCT (PRODUCT_ID)
    /
