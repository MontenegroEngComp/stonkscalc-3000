# 📈 StonksCalc 3000

> A calculadora científica que acredita em você.

Calculadora científica completa com **motor matemático em Java** (sem nenhuma dependência externa), **interface em React** e reações em formato de **meme** para cada resultado. Ela faz contas, deriva, integra, calcula limites, resolve equações e desenha gráficos interativos.

| Resultado | Reação |
| --- | --- |
| `42` | 🌌 *A RESPOSTA para a vida, o universo e tudo mais* |
| `1/0` | 💥 *VOCÊ DIVIDIU POR ZERO: o universo agradece o caos* |
| resultado maior que o anterior | 📈 *STONKS* |
| resultado negativo | 📉 *NEGATIVADO: igual meu nome no Serasa* |
| erro de sintaxe | 🔥🐶☕ *THIS IS FINE* |

---

## ✨ Funcionalidades

### 🧮 Calculadora
- Operações básicas, potências (`^`), fatorial (`!`), porcentagem (`%`), notação científica (`1.5E3`)
- **Trigonometria** (sin, cos, tan, sec, csc, cot e inversas) nos modos **RAD** e **DEG**
- **Hiperbólicas** (sinh, cosh, tanh, sech, csch, coth e inversas)
- **Logaritmos e raízes**: ln, log, log na base b, log2, exp, √, ∛, raiz n-ésima
- **Aritmética**: abs, floor, ceil, round, trunc, mod, MDC, MMC, min, max, hipotenusa
- **Combinatória**: nCr, nPr, função Gama Γ(x)
- **Estatística**: média, mediana, variância, desvio padrão (amostral e populacional)
- Memória (MC, MR, M+, M−), `ans` (resultado anterior), histórico e tecla **2nd**
- Multiplicação implícita (`2x`, `3(1+2)`, `2pi`) e nomes em português (`sen`, `tg`, `raiz`, `mdc`…)

### ∫ Cálculo
- **Derivada simbólica** de qualquer ordem (regras da cadeia, do produto e do quociente), com valor no ponto
- **Integral definida** (Gauss–Kronrod adaptativo), inclusive com limites infinitos: `integral(e^(-x^2), x, -inf, inf)`
- **Limites** bilaterais e laterais, inclusive no infinito (extrapolação de Richardson)
- **Somatório Σ e produtório Π**
- **Resolver equações** `f(x) = g(x)`: encontra todas as raízes reais de um intervalo

Tudo isso também funciona direto na calculadora:

```
deriv(x^3, x, 2)              → 12
integral(sin(x), x, 0, pi)    → 2
limit(sin(x)/x, x, 0)         → 1
sum(1/k^2, k, 1, 1000)        → 1.64393456668
solve(cos(x) - x, x)          → 0.739085133215
```

### 📈 Gráficos
- Várias funções ao mesmo tempo, cada uma com sua cor
- Arrastar para mover, rodinha do mouse para dar zoom e botão de enquadrar
- Marca as **raízes**, mostra os valores sob o cursor e trata assíntotas (`tan(x)`, `1/x`)
- Botão **f′** que plota a derivada da função
- Pinta a **área da integral** calculada na aba Cálculo

### 🎨 Visual
- Estilo neobrutalista, com legendas de meme em fonte Impact
- **Modo Morcego** 🦇 (tema escuro)
- Responsivo (funciona no celular)

---

## 🚀 Como rodar

### Pré-requisitos
- **Java 21+** (JDK) — [download](https://adoptium.net/)
- **Node.js 18+** — [download](https://nodejs.org/)

Não é preciso Maven: o backend compila só com o `javac` do JDK.

### Jeito rápido (tudo em um comando)

```powershell
# Windows (PowerShell)
.\start.ps1
```
```sh
# Linux / macOS
./start.sh
```

Depois, abra **http://localhost:8080** no navegador.

### Modo desenvolvimento (com hot reload)

Em dois terminais:

```powershell
# Terminal 1: backend Java na porta 8080
cd backend
.\build.ps1 -Run        # ou: ./build.sh run
```
```powershell
# Terminal 2: frontend React na porta 5173 (faz proxy de /api para o Java)
cd frontend
npm install
npm run dev
```

Depois, abra **http://localhost:5173**.

### Testes

O script `backend/build.ps1` (ou `build.sh`) compila e roda automaticamente a bateria de testes do motor matemático antes de gerar o `.jar`.

---

## 🏗️ Arquitetura

```
├── backend/                         Java 21, sem dependências
│   └── src/main/java/com/stonkscalc/
│       ├── Main.java                sobe o servidor HTTP
│       ├── api/
│       │   ├── ApiServer.java       rotas /api/* + serve o frontend compilado
│       │   ├── CalcService.java     regras de cada endpoint
│       │   └── Json.java            parser/serializador JSON mínimo
│       └── engine/
│           ├── Lexer / Parser       texto → árvore sintática (AST)
│           ├── Evaluator            AST → número
│           ├── MathFunctions        funções matemáticas
│           ├── Derivative           derivação simbólica
│           ├── Simplifier           simplificação algébrica
│           ├── Calculus             integrais, limites, raízes (numérico)
│           └── Formatter            número/AST → texto
└── frontend/                        React 18 + Vite
    └── src/
        ├── App.jsx                  abas, tema, estado global
        ├── memes.js                 escolhe a reação de cada resultado
        └── components/
            ├── Calculator.jsx       visor, teclado, memória, histórico
            ├── Graph.jsx            plotter em canvas
            ├── CalculusPanel.jsx    derivada, integral, limite, Σ, equações
            └── Manual.jsx           lista de funções (vinda da API)
```

### API

| Método | Rota | Corpo | Resposta |
| --- | --- | --- | --- |
| GET | `/api/health` | — | `{ok, status}` |
| GET | `/api/functions` | — | catálogo de funções |
| POST | `/api/evaluate` | `{expression, angleMode, variables}` | `{value, display, infinite}` |
| POST | `/api/derivative` | `{expression, variable, order, at}` | `{derivative, steps, valueAt}` |
| POST | `/api/plot` | `{functions[], xMin, xMax, samples, roots}` | `{series: [{ys[], roots[]}]}` |
| POST | `/api/roots` | `{expression, a, b}` | `{roots[]}` |

Exemplo:

```sh
curl -X POST localhost:8080/api/evaluate \
  -H "Content-Type: application/json" \
  -d '{"expression": "sin(30) + 5!", "angleMode": "DEG"}'
# {"ok":true,"value":120.5,"display":"120.5","infinite":false}
```

---

## ⌨️ Atalhos

| Tecla | Ação |
| --- | --- |
| `Enter` | calcula |
| `Esc` | limpa |
| digitar após o `=` | operador continua a partir de `ans`; número começa uma conta nova |
