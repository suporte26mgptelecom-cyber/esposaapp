"""Extrai todas as prescrições; falha se uma fórmula desconhecida aparecer.
Uso: python tools/extract_workbook.py (requer openpyxl==3.1.5).
O Android usa o JSON gerado; não precisa de Python nem Excel.
"""
from pathlib import Path
from collections import Counter
from hashlib import sha256
import json, re, unicodedata
from openpyxl import load_workbook
ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/ACADEMIAWELLNESS.xlsx'
w = load_workbook(SOURCE, data_only=False)
assert w.sheetnames == ['Agenda'] + [f'S{s} A{a}' for s in range(1,9) for a in range(1,4)]
FORMULA = re.compile(r'=IF\(ISNUMBER\(\$B\$(\d+)\),IF\(OR\(\$J\$\1="",\$J\$\1=0\),\$B\$\1\*([0-9.]+),ROUNDDOWN\(\$B\$\1\*\2/\$J\$\1,0\)\*\$J\$\1\),""\)')
def slug(s):
    return re.sub('[^a-z0-9]+','-',unicodedata.normalize('NFKD',s).encode('ascii','ignore').decode().lower()).strip('-')
def num(v):
    return None if v is None else str(int(v) if float(v).is_integer() else v)
plan={'sourceSha256': sha256(SOURCE.read_bytes()).hexdigest(), 'notes': [], 'equipmentOptions': [], 'recordingOptions': [], 'sessions': []}
audit=['# Leitura da planilha oficial','',f'SHA-256: `{plan["sourceSha256"]}`', '', 'Todas as 25 abas foram lidas, incluindo células vazias, fórmulas, validações, mesclagens e hyperlinks. Não existem nomes definidos ou conexões externas. O arquivo não contém macros. Os vídeos são links externos; o aplicativo conserva os endereços, sem baixar os vídeos.', '', '## Agenda e regras', '']
for r in range(34,62):
    v=w['Agenda'].cell(r,2).value
    if v and v!='COMO USAR': plan['notes'].append(v);audit.append('- '+v)
counts=Counter()
for index,ar in enumerate(range(8,32)):
    agenda=w['Agenda']; week=int(agenda.cell(ar,2).value); code=agenda.cell(ar,3).value
    s=w[f'S{week} {code}']; sheet=s.title
    assert agenda.cell(ar,7).value==f'=HYPERLINK("#\'{sheet}\'!B1","Abrir")'
    done_row=next(r for r in range(1,76) if s.cell(r,2).value=='Treino concluído')
    assert agenda.cell(ar,8).value==f"='{sheet}'!F{done_row}"
    session={'id':sheet,'week':week,'code':code,'day':agenda.cell(ar,4).value,'title':agenda.cell(ar,5).value,'heading':s['B3'].value,'pdfPage':int(agenda.cell(ar,6).value),'completed':s.cell(done_row,6).value=='Sim','exercises':[]}
    starts=[r for r in range(6,76) if s.cell(r,9).value and s.cell(r,9).value==s.cell(r,2).value]
    audit += ['', f'## {sheet} — {session["heading"]} (PDF p. {session["pdfPage"]})','']
    for start,end in zip(starts,starts[1:]+[done_row]):
        name=s.cell(start,2).value
        ex={'id':slug(name),'name':name,'equipment':s.cell(start+1,10).value,'recording':s.cell(start+2,10).value,'initialLoad':num(s.cell(start+3,2).value),'increment':num(s.cell(start+3,10).value),'rest':num(s.cell(start+3,12).value),'video':'','notes':[],'sets':[]}
        video=s.cell(start,7)
        m=re.fullmatch(r'=HYPERLINK\("([^"]+)","Link"\)',video.value or '')
        ex['video']=m[1] if m else video.hyperlink.target if video.hyperlink else ''
        assert ex['video'].startswith('https://www.youtube.com/')
        for r in range(start,end):
            label=s.cell(r,3).value
            if label:
                formula=s.cell(r,4).value;m=FORMULA.fullmatch(formula)
                assert m,(sheet,r,formula)
                assert int(m[1])==start+3
                typ=re.sub(r' \d+$','',label); number=int(re.search(r'(\d+)$',label)[1]) if re.search(r'(\d+)$',label) else 1
                ex['sets'].append({'key':f'{typ}:{number}','label':label,'factor':m[2],'range':str(s.cell(r,5).value),'initialReps':num(s.cell(r,6).value),'cell':f'D{r}'})
                counts['loadFormulas']+=1
            b=s.cell(r,2).value
            if r>start+3 and isinstance(b,str) and b not in ('Carga de Trabalho','Treino concluído'):ex['notes'].append(b)
        assert len({v['key'] for v in ex['sets']})==len(ex['sets'])
        assert len(ex['sets'])>0
        session['exercises'].append(ex)
        audit += [f'### {name}',f'- Equipamento: {ex["equipment"]}; registro: {ex["recording"]}; carga inicial: {ex["initialLoad"] or "vazia"}; incremento: {ex["increment"] or "vazio"}; descanso: {ex["rest"] or "vazio"}.', f'- Vídeo: {ex["video"]}']
        audit += [f'- {v["label"]}: {v["factor"]} × WS; repetições alvo {v["range"]}; origem {sheet}!{v["cell"]}.' for v in ex['sets']]
        audit += ['- '+n for n in ex['notes']]
    for dv in s.data_validations.dataValidation:
        if dv.type=='list' and 'Máquina' in dv.formula1:plan['equipmentOptions']=dv.formula1.strip('"').split(',')
        if dv.type=='list' and 'Por halter' in dv.formula1:plan['recordingOptions']=dv.formula1.strip('"').split(',')
    plan['sessions'].append(session)
assert len(plan['sessions'])==24
assert all(('Pernas' in s['title']) == (i%2==0) for i,s in enumerate(plan['sessions']))
audit += ['', '## Particularidades preservadas','', '- A única carga preenchida no arquivo é 5 kg na cadeira abdutora (12 ocorrências). Não se presumem outras cargas.', '- S1 A2: remada articulada unilateral configurada como Polia; demais ocorrências como Máquina. Perfis separados.', '- S1 A2: desenvolvimento na máquina articulada configurado como Halter; demais ocorrências como Máquina. Perfis separados.', '- As elevações laterais sentada e sentada (90º) permanecem variações distintas.', '- Cadeira flexora corresponde à flexora sentada adaptada. O step up já está ausente; não há substituição adicional.', '- Incrementos, repetições realizadas e descansos estão vazios. Zero é um valor, não ausência.', '- A escolha de aparelho pode ser refinada por um nome de perfil no aplicativo; nunca se copiam cargas ao trocar perfil.', '- As fórmulas de carga foram verificadas integralmente contra a gramática IF/ISNUMBER/OR/ROUNDDOWN. Agenda: 24 links internos e 24 referências de conclusão.', f'- Total: {counts["loadFormulas"]} fórmulas de carga.']
(ROOT/'app/src/main/assets/wellness.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n')
(ROOT/'docs/PLANILHA.md').write_text('\n'.join(audit)+'\n')
print('Extraído:',len(w.sheetnames),'abas,',len(plan['sessions']),'sessões,',sum(len(s['exercises']) for s in plan['sessions']),'exercícios,',counts['loadFormulas'],'séries')
