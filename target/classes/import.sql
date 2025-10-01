-- Inserir FichaTecnica
INSERT INTO FichaTecnica (historia, principaisJogos, premiosEReconhecimentos) VALUES
                                                                                  ('Fundada em 1991, conhecida por criar experiências single-player ricas em narrativa.', 'The Elder Scrolls, Fallout', 'Vários prêmios de Jogo do Ano'),
                                                                                  ('Estúdio polonês famoso por suas adaptações de obras literárias para o mundo dos jogos.', 'The Witcher, Cyberpunk 2077', 'The Game Award for Game of the Year'),
                                                                                  ('Uma das maiores empresas de jogos do mundo, com franquias icônicas.', 'Mario, Zelda, Pokémon', 'Inúmeros prêmios ao longo de décadas');

-- Inserir Desenvolvedora
INSERT INTO Desenvolvedora (nome, dataDeFundacao, paisDeOrigem, ficha_tecnica_id) VALUES
                                                                                      ('Bethesda Game Studios', '1991-06-28', 'Estados Unidos', 1),
                                                                                      ('CD Projekt Red', '2002-02-01', 'Polônia', 2),
                                                                                      ('Nintendo', '1889-09-23', 'Japão', 3);

-- Inserir Genero
INSERT INTO Genero (nome, descricao) VALUES
                                         ('RPG', 'Role-playing game, um gênero onde os jogadores assumem os papéis de personagens em um cenário fictício.'),
                                         ('Ação', 'Gênero focado em desafios físicos, incluindo coordenação e tempo de reação.'),
                                         ('Aventura', 'Gênero com foco na exploração e solução de quebra-cabeças.'),
                                         ('Mundo Aberto', 'Jogos que apresentam um vasto mundo para o jogador explorar livremente.');

-- Inserir Jogo
INSERT INTO Jogo (titulo, descricao, anoLancamento, classificacaoIndicativa, desenvolvedora_id) VALUES
                                                                                                    ('The Elder Scrolls V: Skyrim', 'Um RPG de mundo aberto onde você é o Dragonborn, um herói com o poder de absorver almas de dragão.', 2011, 'DEZOITO', 1),
                                                                                                    ('The Witcher 3: Wild Hunt', 'Jogue como Geralt de Rivia, um caçador de monstros, em busca de sua filha adotiva em um vasto mundo aberto.', 2015, 'DEZESSEIS', 2),
                                                                                                    ('The Legend of Zelda: Breath of the Wild', 'Explore o vasto reino de Hyrule para derrotar Calamity Ganon.', 2017, 'DEZ', 3);

-- Associações jogo-genero (Many-to-Many)
INSERT INTO jogo_genero (jogo_id, genero_id) VALUES (1, 1), (1, 4);
INSERT INTO jogo_genero (jogo_id, genero_id) VALUES (2, 1), (2, 4);
INSERT INTO jogo_genero (jogo_id, genero_id) VALUES (3, 2), (3, 3), (3, 4);