# Copias visuais de diagramas

A fonte normativa dos fluxos e da arquitetura e o Mermaid em [`../diagramas.md`](../diagramas.md).

Os arquivos `.json`, `.html` desta pasta sao copias usadas em verificacao visual. Varios ainda descrevem `POST /api/sales` como passo da UI do PDV, misturam status local (`PENDING`/`SENT`/`CONFLICT`/`ERROR`) com status servidor de conflito (`ACCEPTED`/`REJECTED`) ou omitem o persist-first da finalizacao atual.

Nao use estes artefatos como contrato. Se um fluxo critico mudar, atualize primeiro `diagramas.md` e so depois regenere as copias visuais.

Indice do restante da documentacao: [`../README.md`](../README.md).
