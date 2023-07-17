#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon May  2 17:27:06 2022

@author: andrea
"""

import pandas as pd
import glob

csv_list = glob.glob('*/*csv')

for i, datafile in enumerate(csv_list):
    if i==0:
        df = pd.read_csv(datafile, index_col=None, dtype=object)
    else:
        df2 = pd.read_csv(datafile,  index_col=None, dtype=object)
        df = pd.concat([df, df2],ignore_index=True)

df.to_csv("combined_searches.csv")