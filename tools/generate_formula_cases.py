"""Fixtures independentes do Kotlin: interpretam os fatores diretamente de TODAS as fórmulas Excel."""
from pathlib import Path
from decimal import Decimal, ROUND_DOWN
from openpyxl import load_workbook
import re,json
root=Path(__file__).resolve().parents[1]
w=load_workbook(root/'docs/ACADEMIAWELLNESS.xlsx')
scenarios=[(None,None),('0',None),('5',None),('100',None),('37.5','2.5'),('15','0'),('5','0.01')]
cases=[]
for s in w:
 if s.title=='Agenda':continue
 for row in s:
  cell=row[3]
  if cell.data_type!='f':continue
  f=cell.value
  factors=re.findall(r'\$B\$\d+\*([0-9.]+)',f)
  assert len(factors)==2 and factors[0]==factors[1]
  assert 'ROUNDDOWN' in f and 'ISNUMBER' in f
  for load,inc in scenarios:
   expected=None
   if load is not None:
    value=Decimal(load)*Decimal(factors[0])
    if inc is not None and Decimal(inc)!=0:
     value=(value/Decimal(inc)).to_integral_value(rounding=ROUND_DOWN)*Decimal(inc)
    expected=format(value.normalize(),'f')
   cases.append(dict(sheet=s.title,cell=cell.coordinate,load=load,increment=inc,factor=factors[0],expected=expected))
(root/'app/src/test/resources/formula-cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n')
print(len(cases),'casos gerados a partir das fórmulas originais')
