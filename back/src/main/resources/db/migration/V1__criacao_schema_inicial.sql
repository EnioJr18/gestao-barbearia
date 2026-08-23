CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_usuarios_email ON usuarios (LOWER(email));

CREATE TABLE categorias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_categorias_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_categorias_nome CHECK (BTRIM(nome) <> '')
);
CREATE UNIQUE INDEX uk_categorias_tipo_nome ON categorias (tipo, LOWER(nome));
CREATE INDEX idx_categorias_ativas ON categorias (ativa);

CREATE TABLE servicos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    preco_atual NUMERIC(12,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_servicos_nome CHECK (BTRIM(nome) <> ''),
    CONSTRAINT ck_servicos_preco CHECK (preco_atual >= 0)
);
CREATE UNIQUE INDEX uk_servicos_nome ON servicos (LOWER(nome));
CREATE INDEX idx_servicos_ativos ON servicos (ativo);

CREATE TABLE despesas_recorrentes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    descricao VARCHAR(255) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    categoria_id UUID NOT NULL,
    dia_vencimento SMALLINT NOT NULL,
    periodicidade VARCHAR(20) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_despesas_recorrentes_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id) ON DELETE RESTRICT,
    CONSTRAINT ck_despesas_recorrentes_valor CHECK (valor > 0),
    CONSTRAINT ck_despesas_recorrentes_dia CHECK (dia_vencimento BETWEEN 1 AND 31),
    CONSTRAINT ck_despesas_recorrentes_periodicidade CHECK (periodicidade IN ('MENSAL')),
    CONSTRAINT ck_despesas_recorrentes_datas CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_despesas_recorrentes_descricao CHECK (BTRIM(descricao) <> '')
);
CREATE INDEX idx_despesas_recorrentes_categoria ON despesas_recorrentes (categoria_id);
CREATE INDEX idx_despesas_recorrentes_ativas ON despesas_recorrentes (ativa);

CREATE TABLE movimentacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(20) NOT NULL,
    origem VARCHAR(30) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    data_movimentacao DATE NOT NULL,
    categoria_id UUID NOT NULL,
    forma_pagamento VARCHAR(20) NOT NULL,
    servico_id UUID,
    nome_servico_snapshot VARCHAR(120),
    valor_servico_snapshot NUMERIC(12,2),
    despesa_recorrente_id UUID,
    observacao VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimentacoes_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacoes_servico FOREIGN KEY (servico_id) REFERENCES servicos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacoes_despesa_recorrente FOREIGN KEY (despesa_recorrente_id) REFERENCES despesas_recorrentes (id) ON DELETE RESTRICT,
    CONSTRAINT ck_movimentacoes_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_movimentacoes_origem CHECK (origem IN ('MANUAL', 'SERVICO', 'DESPESA_RECORRENTE')),
    CONSTRAINT ck_movimentacoes_forma_pagamento CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'DEBITO', 'CREDITO', 'OUTRO')),
    CONSTRAINT ck_movimentacoes_valor CHECK (valor > 0),
    CONSTRAINT ck_movimentacoes_descricao CHECK (BTRIM(descricao) <> ''),
    CONSTRAINT ck_movimentacoes_snapshot_servico CHECK (
        (servico_id IS NULL AND nome_servico_snapshot IS NULL AND valor_servico_snapshot IS NULL)
        OR
        (servico_id IS NOT NULL AND nome_servico_snapshot IS NOT NULL AND valor_servico_snapshot IS NOT NULL AND valor_servico_snapshot >= 0)
    ),
    CONSTRAINT ck_movimentacoes_origem_despesa_recorrente CHECK (
        (origem = 'DESPESA_RECORRENTE' AND despesa_recorrente_id IS NOT NULL)
        OR
        (origem <> 'DESPESA_RECORRENTE')
    )
);
CREATE INDEX idx_movimentacoes_data ON movimentacoes (data_movimentacao);
CREATE INDEX idx_movimentacoes_tipo_data ON movimentacoes (tipo, data_movimentacao);
CREATE INDEX idx_movimentacoes_categoria_data ON movimentacoes (categoria_id, data_movimentacao);
CREATE INDEX idx_movimentacoes_forma_pagamento_data ON movimentacoes (forma_pagamento, data_movimentacao);
CREATE INDEX idx_movimentacoes_servico_data ON movimentacoes (servico_id, data_movimentacao);
CREATE INDEX idx_movimentacoes_despesa_recorrente ON movimentacoes (despesa_recorrente_id);
CREATE UNIQUE INDEX uk_movimentacoes_despesa_recorrente_data ON movimentacoes (despesa_recorrente_id, data_movimentacao) WHERE despesa_recorrente_id IS NOT NULL;

CREATE TABLE fechamentos_diarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data DATE NOT NULL,
    observacao VARCHAR(1000),
    usuario_id UUID NOT NULL,
    fechado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fechamentos_diarios_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX uk_fechamentos_diarios_data ON fechamentos_diarios (data);
CREATE INDEX idx_fechamentos_diarios_usuario ON fechamentos_diarios (usuario_id);
CREATE INDEX idx_fechamentos_diarios_data ON fechamentos_diarios (data);

CREATE TABLE backup_execucoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    arquivo VARCHAR(500),
    tamanho_bytes BIGINT,
    inicio_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fim_em TIMESTAMPTZ,
    erro TEXT,
    CONSTRAINT ck_backup_execucoes_tipo CHECK (tipo IN ('MANUAL', 'AUTOMATICO', 'RESTAURACAO')),
    CONSTRAINT ck_backup_execucoes_status CHECK (status IN ('INICIADO', 'SUCESSO', 'FALHA')),
    CONSTRAINT ck_backup_execucoes_tamanho CHECK (tamanho_bytes IS NULL OR tamanho_bytes >= 0),
    CONSTRAINT ck_backup_execucoes_datas CHECK (fim_em IS NULL OR fim_em >= inicio_em)
);
CREATE INDEX idx_backup_execucoes_inicio ON backup_execucoes (inicio_em);
CREATE INDEX idx_backup_execucoes_status ON backup_execucoes (status);
