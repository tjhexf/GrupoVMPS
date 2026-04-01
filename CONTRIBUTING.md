# Estrutura do Projeto:

### Este repositório segue esse modelo:

- A branch main é protegida
- Apenas 1 maintainer pode fazer merge na main

Todos os outros desenvolvedores:

- Criam branches
- Abrem Pull Requests (PRs)

### Workflow:

- Clone o repositório remoto
- Atualize o repositório local:
``` 
    git checkout main
    git pull origin main
```
- Crie uma nova branch:
```
    git checkout -b <usuario>/<nome-branch>
```
- Faça as alterações
- Envie as mudanças:
```
    git push origin <usuario>/<nome-branch>
```
- Abra um pull request pela interface web do github.
- Aguarde as conversas e aprovações do seu código, e só então ele será adicionado para a main.
