-- ============================================================
-- Barbeiro Financeiro - Migration V2
-- Evolução do modelo financeiro para operação com caixa,
-- vendas, clientes, catálogo de itens e pagamentos divididos.
--
-- IMPORTANTE:
-- - V1 não deve ser alterada.
-- - Esta migration pressupõe que V1 já foi aplicada.
-- - Regras que envolvem múltiplas linhas/transações permanecem
--   na camada de domínio da aplicação.
-- ============================================================

-- ============================================================
-- 1. CLIENTES
-- ============================================================

CREATE TABLE clientes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(30),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_clientes_nome
        CHECK (BTRIM(nome) <> '')
);

CREATE INDEX idx_clientes_nome
    ON clientes (LOWER(nome));


-- ============================================================
-- 2. CATÁLOGO DE ITENS
--    Um catálogo único para SERVICO e PRODUTO.
-- ============================================================

CREATE TABLE itens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    preco NUMERIC(12,2) NOT NULL,
    estoque INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_itens_nome
        CHECK (BTRIM(nome) <> ''),

    CONSTRAINT ck_itens_tipo
        CHECK (tipo IN ('SERVICO', 'PRODUTO')),

    CONSTRAINT ck_itens_preco
        CHECK (preco >= 0),

    CONSTRAINT ck_itens_estoque_por_tipo
        CHECK (
            (tipo = 'SERVICO' AND estoque IS NULL)
            OR
            (tipo = 'PRODUTO' AND estoque >= 0)
        )
);

CREATE UNIQUE INDEX uk_itens_nome
    ON itens (LOWER(nome));

CREATE INDEX idx_itens_tipo_ativo
    ON itens (tipo, ativo);


-- ============================================================
-- 3. CAIXAS
-- ============================================================

CREATE TABLE caixas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_caixa DATE NOT NULL,
    valor_inicial NUMERIC(12,2) NOT NULL,
    valor_apurado NUMERIC(12,2),
    diferenca NUMERIC(12,2),
    status VARCHAR(20) NOT NULL,
    usuario_abertura_id UUID NOT NULL,
    usuario_fechamento_id UUID,
    aberto_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fechado_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_caixas_usuario_abertura
        FOREIGN KEY (usuario_abertura_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_caixas_usuario_fechamento
        FOREIGN KEY (usuario_fechamento_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_caixas_valor_inicial
        CHECK (valor_inicial >= 0),

    CONSTRAINT ck_caixas_valor_apurado
        CHECK (valor_apurado IS NULL OR valor_apurado >= 0),

    CONSTRAINT ck_caixas_status
        CHECK (status IN ('ABERTO', 'FECHADO')),

    CONSTRAINT ck_caixas_estado
        CHECK (
            (
                status = 'ABERTO'
                AND valor_apurado IS NULL
                AND diferenca IS NULL
                AND usuario_fechamento_id IS NULL
                AND fechado_em IS NULL
            )
            OR
            (
                status = 'FECHADO'
                AND valor_apurado IS NOT NULL
                AND diferenca IS NOT NULL
                AND usuario_fechamento_id IS NOT NULL
                AND fechado_em IS NOT NULL
            )
        ),

    CONSTRAINT ck_caixas_datas
        CHECK (fechado_em IS NULL OR fechado_em >= aberto_em)
);

CREATE UNIQUE INDEX uk_caixas_data
    ON caixas (data_caixa);

-- Garante apenas um caixa aberto no sistema.
CREATE UNIQUE INDEX uk_caixas_aberto
    ON caixas (status)
    WHERE status = 'ABERTO';

CREATE INDEX idx_caixas_status_data
    ON caixas (status, data_caixa);


-- ============================================================
-- 4. VENDAS
-- ============================================================

CREATE TABLE vendas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID,
    caixa_id UUID NOT NULL,
    data_venda DATE NOT NULL,
    valor_total NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendas_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES clientes (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_vendas_caixa
        FOREIGN KEY (caixa_id)
        REFERENCES caixas (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_vendas_valor_total
        CHECK (valor_total > 0)
);

CREATE INDEX idx_vendas_caixa_data
    ON vendas (caixa_id, data_venda);

CREATE INDEX idx_vendas_cliente_data
    ON vendas (cliente_id, data_venda);

CREATE INDEX idx_vendas_data
    ON vendas (data_venda);


-- ============================================================
-- 5. ITENS DA VENDA
-- ============================================================

CREATE TABLE itens_venda (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venda_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,

    CONSTRAINT fk_itens_venda_venda
        FOREIGN KEY (venda_id)
        REFERENCES vendas (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_itens_venda_item
        FOREIGN KEY (item_id)
        REFERENCES itens (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_itens_venda_quantidade
        CHECK (quantidade > 0),

    CONSTRAINT ck_itens_venda_preco
        CHECK (preco_unitario >= 0),

    CONSTRAINT ck_itens_venda_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT ck_itens_venda_calculo
        CHECK (subtotal = quantidade * preco_unitario)
);

CREATE UNIQUE INDEX uk_itens_venda_venda_item
    ON itens_venda (venda_id, item_id);

CREATE INDEX idx_itens_venda_venda
    ON itens_venda (venda_id);

CREATE INDEX idx_itens_venda_item
    ON itens_venda (item_id);


-- ============================================================
-- 6. PAGAMENTOS DA VENDA
--    Permite pagamento único ou dividido.
-- ============================================================

CREATE TABLE vendas_pagamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venda_id UUID NOT NULL,
    forma_pagamento VARCHAR(20) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendas_pagamentos_venda
        FOREIGN KEY (venda_id)
        REFERENCES vendas (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_vendas_pagamentos_forma
        CHECK (
            forma_pagamento IN (
                'DINHEIRO',
                'PIX',
                'CARTAO_CREDITO',
                'CARTAO_DEBITO'
            )
        ),

    CONSTRAINT ck_vendas_pagamentos_valor
        CHECK (valor > 0)
);

CREATE UNIQUE INDEX uk_vendas_pagamentos_venda_forma
    ON vendas_pagamentos (venda_id, forma_pagamento);

CREATE INDEX idx_vendas_pagamentos_venda
    ON vendas_pagamentos (venda_id);

CREATE INDEX idx_vendas_pagamentos_forma
    ON vendas_pagamentos (forma_pagamento);


-- ============================================================
-- 7. EVOLUÇÃO DAS MOVIMENTAÇÕES
-- ============================================================

-- Remove constraints da V1 que não representam mais o domínio V2.
ALTER TABLE movimentacoes
    DROP CONSTRAINT IF EXISTS ck_movimentacoes_origem;

ALTER TABLE movimentacoes
    DROP CONSTRAINT IF EXISTS ck_movimentacoes_forma_pagamento;

ALTER TABLE movimentacoes
    DROP CONSTRAINT IF EXISTS ck_movimentacoes_snapshot_servico;

ALTER TABLE movimentacoes
    DROP CONSTRAINT IF EXISTS ck_movimentacoes_origem_despesa_recorrente;

ALTER TABLE movimentacoes
    DROP CONSTRAINT IF EXISTS fk_movimentacoes_servico;

-- O índice abaixo depende de despesa_recorrente_id, que continua existindo,
-- portanto permanece válido e não precisa ser recriado.

ALTER TABLE movimentacoes
    DROP COLUMN IF EXISTS servico_id,
    DROP COLUMN IF EXISTS nome_servico_snapshot,
    DROP COLUMN IF EXISTS valor_servico_snapshot;

ALTER TABLE movimentacoes
    ADD COLUMN caixa_id UUID;

ALTER TABLE movimentacoes
    ADD CONSTRAINT fk_movimentacoes_caixa
        FOREIGN KEY (caixa_id)
        REFERENCES caixas (id)
        ON DELETE RESTRICT;

ALTER TABLE movimentacoes
    ADD CONSTRAINT ck_movimentacoes_origem_v2
        CHECK (
            origem IN (
                'MANUAL',
                'DESPESA_RECORRENTE',
                'AJUSTE',
                'OUTRA'
            )
        );

ALTER TABLE movimentacoes
    ADD CONSTRAINT ck_movimentacoes_forma_pagamento_v2
        CHECK (
            forma_pagamento IN (
                'DINHEIRO',
                'PIX',
                'CARTAO_CREDITO',
                'CARTAO_DEBITO'
            )
        );

ALTER TABLE movimentacoes
    ADD CONSTRAINT ck_movimentacoes_recorrencia_v2
        CHECK (
            (
                origem = 'DESPESA_RECORRENTE'
                AND despesa_recorrente_id IS NOT NULL
            )
            OR
            (
                origem <> 'DESPESA_RECORRENTE'
                AND despesa_recorrente_id IS NULL
            )
        );

CREATE INDEX idx_movimentacoes_caixa_data
    ON movimentacoes (caixa_id, data_movimentacao);


-- ============================================================
-- 8. EVOLUÇÃO DAS DESPESAS RECORRENTES
-- ============================================================

ALTER TABLE despesas_recorrentes
    ADD COLUMN forma_pagamento VARCHAR(20) NOT NULL DEFAULT 'DINHEIRO';

ALTER TABLE despesas_recorrentes
    ADD CONSTRAINT ck_despesas_recorrentes_forma_pagamento
        CHECK (
            forma_pagamento IN (
                'DINHEIRO',
                'PIX',
                'CARTAO_CREDITO',
                'CARTAO_DEBITO'
            )
        );


-- ============================================================
-- 9. ÍNDICES COMPLEMENTARES
--    Os índices de categoria/tipo/forma de pagamento já existem
--    desde a V1 e são reaproveitados.
-- ============================================================


-- ============================================================
-- 10. REMOÇÃO DAS ESTRUTURAS SUBSTITUÍDAS
--
-- A V2 substitui:
--   servicos           -> itens
--   fechamentos_diarios -> caixas
-- ============================================================

DROP TABLE IF EXISTS fechamentos_diarios;
DROP TABLE IF EXISTS servicos;


-- ============================================================
-- FIM DA V2
-- ============================================================
